import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useSaisieStore } from "@/stores/saisie.ts";
import type { Libelle, Operation } from "@/types/comptes.ts";

const operation: Operation = {
    id: 7,
    compteId: 1,
    date: "2026-10-13",
    type: "DEPENSE",
    montant: 42,
    categorieId: 3,
    libelleId: 5,
    note: null,
    recurrenceId: null,
};

const libelle: Libelle = {
    id: 5,
    nom: "Supermarché",
    categorieId: 3,
    fourni: false,
    masque: false,
    archive: false,
};

const octobre = { dateDebut: "2026-10-01", dateFin: "2026-10-31" };
const novembre = { dateDebut: "2026-11-01", dateFin: "2026-11-30" };

describe("store saisie", () => {
    beforeEach(() => {
        setActivePinia(createPinia());
    });

    it("est fermé au départ", () => {
        const store = useSaisieStore();
        expect(store.ouverte).toBe(false);
        expect(store.aModifier).toBeNull();
        expect(store.periodeAffichee).toBeNull();
    });

    it("ouvre une saisie vierge, même après une modification", () => {
        const store = useSaisieStore();
        store.modifier(operation, libelle);

        store.ouvrir();

        expect(store.ouverte).toBe(true);
        expect(store.aModifier).toBeNull();
    });

    it("ouvre la saisie sur l'opération à modifier", () => {
        const store = useSaisieStore();

        store.modifier(operation, libelle);

        expect(store.ouverte).toBe(true);
        expect(store.aModifier).toEqual({ operation, libelle });
    });

    it("oublie l'opération à modifier à la fermeture", () => {
        const store = useSaisieStore();
        store.modifier(operation, null);

        store.fermer();

        expect(store.ouverte).toBe(false);
        expect(store.aModifier).toBeNull();
    });

    it("compte les opérations enregistrées", () => {
        const store = useSaisieStore();
        store.signalerEnregistrement();
        store.signalerEnregistrement();
        expect(store.nbEnregistrees).toBe(2);
    });

    describe("période affichée", () => {
        it("oublie la période de l'écran quitté", () => {
            const store = useSaisieStore();
            store.afficherPeriode(octobre);

            store.oublierPeriode(octobre);

            expect(store.periodeAffichee).toBeNull();
        });

        it("garde la période de l'écran suivant, déjà affichée", () => {
            const store = useSaisieStore();
            store.afficherPeriode(octobre);
            store.afficherPeriode(novembre);

            store.oublierPeriode(octobre);

            expect(store.periodeAffichee).toEqual(novembre);
        });

        it("compare les périodes par identité, pas par contenu", () => {
            const store = useSaisieStore();
            store.afficherPeriode(octobre);

            store.oublierPeriode({ ...octobre });

            expect(store.periodeAffichee).toEqual(octobre);
        });

        it("ne fait rien pour une période nulle", () => {
            const store = useSaisieStore();
            store.afficherPeriode(octobre);

            store.oublierPeriode(null);

            expect(store.periodeAffichee).toEqual(octobre);
        });
    });
});
