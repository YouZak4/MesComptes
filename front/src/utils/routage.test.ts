import { describe, expect, it } from "vitest";
import { idConnu, idDepuisQuery } from "@/utils/routage.ts";

describe("idDepuisQuery", () => {
    it("lit un id entier positif", () => {
        expect(idDepuisQuery("12")).toBe(12);
    });

    // Chaque cas dans un tableau : it.each déplierait sinon le paramètre répété ["1", "2"]
    it.each([
        [undefined],
        [null],
        [""],
        ["abc"],
        ["0"],
        ["-3"],
        ["1.5"],
        [["1", "2"]],
    ])("renvoie undefined pour %j", (valeur) => {
        expect(idDepuisQuery(valeur)).toBeUndefined();
    });
});

describe("idConnu", () => {
    const connus = new Set([1, 2]);

    it("garde un id connu", () => {
        expect(idConnu("2", connus)).toBe(2);
    });

    it("écarte un id inconnu (élément supprimé depuis)", () => {
        expect(idConnu("3", connus)).toBeUndefined();
    });

    it("écarte un id invalide", () => {
        expect(idConnu("abc", connus)).toBeUndefined();
    });
});
