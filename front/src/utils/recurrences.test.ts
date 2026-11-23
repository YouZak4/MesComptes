import { describe, expect, it } from "vitest";
import { decrireCalendrier, jourDe } from "@/utils/recurrences.ts";

describe("decrireCalendrier", () => {
    it("décrit une récurrence mensuelle", () => {
        expect(decrireCalendrier("MENSUELLE", 5, "2026-01-05")).toBe(
            "Le 5 de chaque mois"
        );
    });

    it("écrit « 1er » pour le premier du mois", () => {
        expect(decrireCalendrier("MENSUELLE", 1, "2026-01-01")).toBe(
            "Le 1er de chaque mois"
        );
    });

    it("prend le mois de la date de début pour une récurrence annuelle", () => {
        expect(decrireCalendrier("ANNUELLE", 12, "2026-03-12")).toBe(
            "Chaque année le 12 mars"
        );
    });
});

describe("jourDe", () => {
    it("lit le jour d'une date ISO", () => {
        expect(jourDe("2026-10-07")).toBe(7);
    });
});
