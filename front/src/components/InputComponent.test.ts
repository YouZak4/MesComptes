import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import InputComponent from "@/components/InputComponent.vue";

import {
    MESSAGE_MOT_DE_PASSE,
    REGLE_MOT_DE_PASSE,
} from "@/utils/validation.ts";

// Monte le champ avec un v-model simulé : chaque saisie remonte dans modelValue
function monter(props: Record<string, unknown> = {}) {
    const wrapper = mount(InputComponent, {
        props: {
            modelValue: "",
            "onUpdate:modelValue": (valeur: string) =>
                wrapper.setProps({ modelValue: valeur }),
            ...props,
        },
    });
    return wrapper;
}

// Dernière validité envoyée au parent
function derniereValidite(wrapper: ReturnType<typeof monter>) {
    return wrapper.emitted("valid")?.at(-1)?.[0];
}

function messageErreur(wrapper: ReturnType<typeof monter>) {
    const erreur = wrapper.find(".erreur");
    return erreur.exists() ? erreur.text() : null;
}

describe("InputComponent", () => {
    it("est invalide au départ quand il est vide", () => {
        const wrapper = monter({ required: true });
        expect(derniereValidite(wrapper)).toBe(false);
        expect(messageErreur(wrapper)).toBeNull();
    });

    it("est invalide pendant la saisie d'une valeur au mauvais format, sans afficher d'erreur", async () => {
        const wrapper = monter({
            required: true,
            regex: REGLE_MOT_DE_PASSE,
            regexMessage: MESSAGE_MOT_DE_PASSE,
        });

        await wrapper.find("input").setValue("MauvaisMotDePasse1");

        expect(derniereValidite(wrapper)).toBe(false);
        expect(messageErreur(wrapper)).toBeNull();
    });

    it("devient valide dès qu'une valeur correcte est saisie", async () => {
        const wrapper = monter({ required: true, regex: REGLE_MOT_DE_PASSE });

        await wrapper.find("input").setValue("Correct-123");

        expect(derniereValidite(wrapper)).toBe(true);
    });

    it("redevient invalide si la saisie ne respecte plus le format", async () => {
        const wrapper = monter({ required: true, regex: REGLE_MOT_DE_PASSE });
        const input = wrapper.find("input");

        await input.setValue("Correct-123");
        await input.setValue("Correct123");

        expect(derniereValidite(wrapper)).toBe(false);
    });

    it("affiche l'erreur de format une fois le champ quitté", async () => {
        const wrapper = monter({
            required: true,
            regex: REGLE_MOT_DE_PASSE,
            regexMessage: MESSAGE_MOT_DE_PASSE,
        });
        const input = wrapper.find("input");

        await input.setValue("MauvaisMotDePasse1");
        await input.trigger("blur");

        expect(messageErreur(wrapper)).toBe(MESSAGE_MOT_DE_PASSE);
        expect(input.attributes("aria-invalid")).toBe("true");
        expect(derniereValidite(wrapper)).toBe(false);
    });

    it("applique le contrôle libre et affiche son message", async () => {
        const wrapper = monter({
            required: true,
            verifier: (valeur: string) =>
                valeur.includes("x") ? null : "Il manque un x.",
        });
        const input = wrapper.find("input");

        await input.setValue("abc");
        await input.trigger("blur");
        expect(messageErreur(wrapper)).toBe("Il manque un x.");
        expect(derniereValidite(wrapper)).toBe(false);

        await input.setValue("abx");
        expect(messageErreur(wrapper)).toBeNull();
        expect(derniereValidite(wrapper)).toBe(true);
    });

    it("signale un champ obligatoire laissé vide une fois quitté", async () => {
        const wrapper = monter({ required: true });

        await wrapper.find("input").trigger("blur");

        expect(messageErreur(wrapper)).toBe("Ce champ est obligatoire.");
    });

    it("contrôle les longueurs minimale et maximale", async () => {
        const wrapper = monter({ minLength: 8, maxLength: 10 });
        const input = wrapper.find("input");
        await input.trigger("blur");

        await input.setValue("court");
        expect(messageErreur(wrapper)).toBe("Minimum 8 caractères.");

        await wrapper.setProps({ modelValue: "beaucoup trop long" });
        expect(messageErreur(wrapper)).toBe("Maximum 10 caractères.");
        expect(derniereValidite(wrapper)).toBe(false);
    });

    it("suit une valeur remise à zéro par le parent", async () => {
        const wrapper = monter({ required: true });

        await wrapper.find("input").setValue("une valeur");
        expect(derniereValidite(wrapper)).toBe(true);

        await wrapper.setProps({ modelValue: "" });
        expect(derniereValidite(wrapper)).toBe(false);
    });

    it("compare à la valeur à recopier", async () => {
        const wrapper = monter({ required: true, correspondA: "Secret-123" });
        const input = wrapper.find("input");

        await input.setValue("Secret-12");
        await input.trigger("blur");
        expect(messageErreur(wrapper)).toBe(
            "Les deux saisies ne correspondent pas."
        );

        await input.setValue("Secret-123");
        expect(messageErreur(wrapper)).toBeNull();
        expect(derniereValidite(wrapper)).toBe(true);
    });

    it("attend la fin d'un clic ailleurs pour afficher l'erreur, sans décaler la page sous le pointeur", async () => {
        vi.useFakeTimers();
        try {
            const wrapper = monter({ required: true });
            const input = wrapper.find("input");

            // Le bouton de la souris s'enfonce sur un autre élément : le champ perd le focus
            document.dispatchEvent(new Event("pointerdown"));
            await input.trigger("blur");
            expect(messageErreur(wrapper)).toBeNull();
            // La validité, elle, n'attend pas
            expect(derniereValidite(wrapper)).toBe(false);

            // Le clic se termine : le message apparaît juste après
            document.dispatchEvent(new Event("pointerup"));
            vi.runAllTimers();
            await nextTick();
            expect(messageErreur(wrapper)).toBe("Ce champ est obligatoire.");
        } finally {
            vi.useRealTimers();
        }
    });

    it("affiche l'erreur tout de suite quand le champ est quitté au clavier", async () => {
        const wrapper = monter({ required: true });

        await wrapper.find("input").trigger("blur");

        expect(messageErreur(wrapper)).toBe("Ce champ est obligatoire.");
    });

    it("retire ses écouteurs de la page quand il disparaît", () => {
        const retirer = vi.spyOn(document, "removeEventListener");
        const wrapper = monter({ required: true });

        wrapper.unmount();

        const types = retirer.mock.calls.map(([type]) => type);
        expect(types).toEqual(
            expect.arrayContaining([
                "pointerdown",
                "pointerup",
                "pointercancel",
            ])
        );
        retirer.mockRestore();
    });

    it("affiche et masque le mot de passe", async () => {
        const wrapper = monter({ type: "password" });
        const input = wrapper.find("input");
        const oeil = wrapper.find("button.oeil");

        expect(input.attributes("type")).toBe("password");
        await oeil.trigger("click");
        expect(input.attributes("type")).toBe("text");
        expect(oeil.attributes("aria-label")).toBe("Masquer le mot de passe");
    });
});
