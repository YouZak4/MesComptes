import { describe, expect, it } from "vitest";
import { messageErreur } from "@/utils/erreurs.ts";
import { nomAffiche } from "@/utils/utilisateur.ts";

describe("messageErreur", () => {
    it("reprend le message renvoyé par l'API", () => {
        const e = { response: { data: { message: "Compte introuvable" } } };
        expect(messageErreur(e, "Erreur")).toBe("Compte introuvable");
    });

    it.each([null, undefined, new Error("réseau"), { response: {} }])(
        "renvoie le message par défaut pour %j",
        (e) => {
            expect(messageErreur(e, "Erreur")).toBe("Erreur");
        }
    );
});

describe("nomAffiche", () => {
    const base = { id: "u1", email: "test@exemple.fr" };

    it("affiche le prénom puis le nom", () => {
        expect(nomAffiche({ ...base, nom: "Dupont", prenom: "Jean" })).toBe(
            "Jean Dupont"
        );
    });

    it("affiche le nom seul sans prénom", () => {
        expect(nomAffiche({ ...base, nom: "Pseudo", prenom: null })).toBe(
            "Pseudo"
        );
    });
});
