import { describe, expect, it } from "vitest";
import { REGLE_IDENTIFIANT, motDePasseConforme } from "@/utils/validation.ts";

describe("motDePasseConforme", () => {
    it.each(["Correct-123", "Abcdef1!", "Un-Mot-De-Passe-De-30-Car-1234"])(
        "accepte %j",
        (motDePasse) => {
            expect(motDePasseConforme(motDePasse)).toBe(true);
        }
    );

    it.each([
        ["", "vide"],
        ["Court-1", "moins de 8 caractères"],
        ["sansmajuscule-1", "sans majuscule"],
        ["SansChiffre-x", "sans chiffre"],
        ["SansSpecial1", "sans caractère spécial"],
        ["Beaucoup-Trop-Long-Pour-La-Regle-1", "plus de 30 caractères"],
    ])("refuse %j (%s)", (motDePasse) => {
        expect(motDePasseConforme(motDePasse)).toBe(false);
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
