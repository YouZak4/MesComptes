import { describe, expect, it } from "vitest";
import { Package, Tv } from "@lucide/vue";
import type { Categorie } from "@/types/comptes.ts";
import {
    COULEUR_REVENUS,
    COULEURS_DEPENSES,
    composantIcone,
    teinteCategorie,
    teinteDe,
} from "@/utils/categories.ts";

function categorie(champs: Partial<Categorie>): Categorie {
    return {
        id: 1,
        nom: "Courses",
        type: "DEPENSE",
        icone: null,
        couleur: null,
        fournie: false,
        masquee: false,
        archive: false,
        ...champs,
    };
}

describe("composantIcone", () => {
    it("renvoie l'icône Lucide du nom stocké", () => {
        expect(composantIcone("tv")).toBe(Tv);
    });

    it.each([null, undefined, "", "🍔", "inconnue"])(
        "renvoie l'icône neutre pour %j",
        (nom) => {
            expect(composantIcone(nom)).toBe(Package);
        }
    );
});

describe("teinteDe", () => {
    it("donne le couple de la marque à un revenu, quelle que soit sa couleur", () => {
        expect(teinteDe("#3F51A3", "REVENU")).toBe(COULEUR_REVENUS);
    });

    it("donne le neutre sans couleur", () => {
        expect(teinteDe(null).couleur).toBe("#6B7079");
    });

    it("retrouve un couple de la charte sans tenir compte de la casse", () => {
        expect(teinteDe("#3f51a3")).toBe(COULEURS_DEPENSES[0]);
    });

    it("calcule la teinte d'une couleur hors charte", () => {
        expect(teinteDe("#123456")).toEqual({
            couleur: "#123456",
            teinte: "color-mix(in srgb, #123456 14%, white)",
        });
    });
});

describe("teinteCategorie", () => {
    it("donne le neutre sans catégorie", () => {
        expect(teinteCategorie(null).couleur).toBe("#6B7079");
    });

    it("utilise la couleur et le type de la catégorie", () => {
        expect(teinteCategorie(categorie({ type: "REVENU" }))).toBe(
            COULEUR_REVENUS
        );
    });
});
