import { beforeEach, describe, expect, it, vi } from "vitest";
import { useReferentiels } from "@/composables/useReferentiels.ts";
import { comptesService } from "@/services/comptesService.ts";
import type { Categorie, Compte, Libelle } from "@/types/comptes.ts";

// Pas d'appel HTTP dans les tests : le service est remplacé
vi.mock("@/services/comptesService.ts", () => ({
    comptesService: {
        getComptes: vi.fn(),
        getCategories: vi.fn(),
        getLibelles: vi.fn(),
    },
}));

const courant: Compte = {
    id: 1,
    nom: "Compte courant",
    soldeInitial: 0,
    dateSoldeInitial: "2026-01-01",
    archive: false,
    solde: 100,
    dernierUtilise: true,
};

const courses: Categorie = {
    id: 3,
    nom: "Courses",
    type: "DEPENSE",
    icone: "shopping-cart",
    couleur: "#3F51A3",
    fournie: true,
    masquee: false,
    archive: false,
};

const supermarche: Libelle = {
    id: 5,
    nom: "Supermarché",
    categorieId: 3,
    fourni: false,
    masque: false,
    archive: false,
};

function operation(champs: {
    libelleId?: number | null;
    categorieId?: number;
    compteId?: number;
    note?: string | null;
}) {
    return {
        compteId: 1,
        categorieId: 3,
        libelleId: null,
        note: null,
        ...champs,
    };
}

async function referentielsCharges() {
    const referentiels = useReferentiels();
    await referentiels.chargerReferentiels();
    return referentiels;
}

describe("useReferentiels", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(comptesService.getComptes).mockResolvedValue([courant]);
        vi.mocked(comptesService.getCategories).mockResolvedValue([courses]);
        vi.mocked(comptesService.getLibelles).mockResolvedValue([supermarche]);
    });

    it("charge comptes, catégories et libellés, indexés par id", async () => {
        const r = await referentielsCharges();

        expect(r.comptes.value).toEqual([courant]);
        expect(r.categories.value.get(3)).toEqual(courses);
        expect(r.libelles.value.get(5)).toEqual(supermarche);
        expect(r.nomsComptes.value.get(1)).toBe("Compte courant");
        expect(r.categorie(3)).toEqual(courses);
    });

    it("donne des référentiels vides avant le chargement", () => {
        const r = useReferentiels();
        expect(r.comptes.value).toEqual([]);
        expect(r.titre(operation({}))).toBe("Opération");
        expect(r.categorie(3)).toBeUndefined();
    });

    describe("titre", () => {
        it("prend le libellé s'il y en a un", async () => {
            const r = await referentielsCharges();
            expect(r.titre(operation({ libelleId: 5 }))).toBe("Supermarché");
        });

        it("prend la catégorie sans libellé", async () => {
            const r = await referentielsCharges();
            expect(r.titre(operation({}))).toBe("Courses");
        });

        it("prend la catégorie si le libellé est inconnu", async () => {
            const r = await referentielsCharges();
            expect(r.titre(operation({ libelleId: 99 }))).toBe("Courses");
        });

        it("tombe sur « Opération » si la catégorie est inconnue", async () => {
            const r = await referentielsCharges();
            expect(r.titre(operation({ categorieId: 99 }))).toBe("Opération");
        });
    });

    describe("details", () => {
        it("donne la catégorie quand le titre est un libellé", async () => {
            const r = await referentielsCharges();
            expect(r.details(operation({ libelleId: 5 }), false)).toBe(
                "Courses"
            );
        });

        it("ne répète pas la catégorie quand elle est déjà le titre", async () => {
            const r = await referentielsCharges();
            expect(r.details(operation({}), false)).toBe("");
        });

        it("ajoute le compte si demandé, puis la note", async () => {
            const r = await referentielsCharges();
            expect(
                r.details(operation({ libelleId: 5, note: "Pain" }), true)
            ).toBe("Courses · Compte courant · Pain");
        });

        it("ignore un compte inconnu", async () => {
            const r = await referentielsCharges();
            expect(r.details(operation({ compteId: 99 }), true)).toBe("");
        });
    });
});
