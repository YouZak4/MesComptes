import { describe, expect, it } from "vitest";
import {
    formatDateCourte,
    formatDateLongue,
    formatEcart,
    formatMontant,
    formatMontantSigne,
    formatPourcentage,
    versDate,
    versIso,
} from "@/utils/format.ts";

// Intl en fr-FR sépare avec des espaces insécables : on les remplace pour lire les attendus
function lisible(texte: string): string {
    return texte.replace(/[  ]/g, " ");
}

describe("formatMontant", () => {
    it("formate un montant positif en euros", () => {
        expect(lisible(formatMontant(1234.5))).toBe("1 234,50 €");
    });

    it("utilise le vrai signe moins pour un montant négatif", () => {
        expect(lisible(formatMontant(-12))).toBe("−12,00 €");
    });
});

describe("formatMontantSigne", () => {
    it("préfixe une dépense par le signe moins", () => {
        expect(lisible(formatMontantSigne(8, "DEPENSE"))).toBe("−8,00 €");
    });

    it("préfixe un revenu par le signe plus", () => {
        expect(lisible(formatMontantSigne(2000, "REVENU"))).toBe("+2 000,00 €");
    });
});

describe("formatEcart", () => {
    it("indique un écart nul comme identique", () => {
        expect(formatEcart(0)).toBe("Identique à la période précédente");
    });

    it("signe un écart positif", () => {
        expect(lisible(formatEcart(15))).toBe("+15,00 € vs période précédente");
    });

    it("signe un écart négatif avec le vrai signe moins", () => {
        expect(lisible(formatEcart(-15))).toBe(
            "−15,00 € vs période précédente"
        );
    });
});

describe("formatPourcentage", () => {
    it("arrondit à l'unité", () => {
        expect(lisible(formatPourcentage(0.483))).toBe("48 %");
    });
});

describe("dates", () => {
    it("lit une date ISO en heure locale, sans décalage d'un jour", () => {
        const date = versDate("2026-10-13");
        expect(date.getFullYear()).toBe(2026);
        expect(date.getMonth()).toBe(9);
        expect(date.getDate()).toBe(13);
    });

    it("refait la date ISO d'une date locale", () => {
        expect(versIso(new Date(2026, 0, 5))).toBe("2026-01-05");
    });

    it("formate une date courte et une date longue", () => {
        expect(formatDateCourte("2026-10-13")).toBe("13 oct.");
        expect(formatDateLongue("2026-10-01")).toBe("1 octobre 2026");
    });
});
