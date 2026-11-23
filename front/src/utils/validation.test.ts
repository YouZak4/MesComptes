import { describe, expect, it } from "vitest";
import {
    MESSAGE_MOT_DE_PASSE,
    MESSAGE_MOT_DE_PASSE_TROP_LONG,
    REGLE_IDENTIFIANT,
    erreurMotDePasse,
} from "@/utils/validation.ts";

describe("erreurMotDePasse", () => {
    it.each([
        "Correct-123",
        "Abcdef1!",
        "Un-Mot-De-Passe-De-30-Car-1234",
        // 64 caractères, la limite
        "A1-" + "a".repeat(61),
        // 71 octets : 34 lettres accentuées de 2 octets
        "A1-" + "é".repeat(34),
    ])("accepte %j", (motDePasse) => {
        expect(erreurMotDePasse(motDePasse)).toBeNull();
    });

    it.each([
        ["", "vide"],
        ["Court-1", "moins de 8 caractères"],
        ["sansmajuscule-1", "sans majuscule"],
        ["SansChiffre-x", "sans chiffre"],
        ["SansSpecial1", "sans caractère spécial"],
        ["A1-" + "a".repeat(62), "plus de 64 caractères"],
    ])("rappelle la règle pour %j (%s)", (motDePasse) => {
        expect(erreurMotDePasse(motDePasse)).toBe(MESSAGE_MOT_DE_PASSE);
    });

    it("signale qu'un mot de passe accentué de moins de 64 caractères dépasse 72 octets", () => {
        expect(erreurMotDePasse("A1-" + "é".repeat(35))).toBe(
            MESSAGE_MOT_DE_PASSE_TROP_LONG
        );
    });
});

describe("REGLE_IDENTIFIANT", () => {
    it.each(["jdupont01", "jean_dupont-2", "a".repeat(30)])(
        "accepte %j",
        (identifiant) => {
            expect(REGLE_IDENTIFIANT.test(identifiant)).toBe(true);
        }
    );

    it.each(["court", "avec espace", "accentué-01", "a".repeat(31)])(
        "refuse %j",
        (identifiant) => {
            expect(REGLE_IDENTIFIANT.test(identifiant)).toBe(false);
        }
    );
});
