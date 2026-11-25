import { describe, expect, it } from "vitest";
import { finDePeriode } from "@/utils/periodes.ts";

describe("finDePeriode", () => {
    it("finit la veille du salaire du mois suivant", () => {
        expect(finDePeriode("2026-09-25")).toBe("2026-10-24");
        expect(finDePeriode("2026-10-25")).toBe("2026-11-24");
    });

    it("donne le mois civil pour un début le 1er", () => {
        expect(finDePeriode("2026-11-01")).toBe("2026-11-30");
        expect(finDePeriode("2026-02-01")).toBe("2026-02-28");
    });

    it("passe à l'année suivante", () => {
        expect(finDePeriode("2026-12-25")).toBe("2027-01-24");
    });

    it("ramène un jour absent du mois suivant à son dernier jour, comme le back", () => {
        expect(finDePeriode("2026-01-31")).toBe("2026-02-27");
        expect(finDePeriode("2028-01-31")).toBe("2028-02-28");
        expect(finDePeriode("2026-03-31")).toBe("2026-04-29");
    });
});
