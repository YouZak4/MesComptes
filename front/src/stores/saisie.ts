import { defineStore } from "pinia";
import { ref, toRaw } from "vue";
import type { Libelle, Operation, Periode } from "@/types/comptes.ts";

// Opération à modifier, avec son libellé (pour afficher son nom sans le rechercher)
export interface OperationAModifier {
    operation: Operation;
    libelle: Libelle | null;
}

// Fenêtre de saisie, ouverte depuis n'importe quel écran (bouton « + » ou touche N),
// ou depuis la liste des opérations pour en modifier une
export const useSaisieStore = defineStore("saisie", () => {
    const ouverte = ref(false);
    const aModifier = ref<OperationAModifier | null>(null);
    // Incrémenté à chaque opération enregistrée : les écrans le surveillent pour se recharger
    const nbEnregistrees = ref(0);
    // Période affichée par l'écran en cours, null hors des écrans de période :
    // une nouvelle opération est datée dans cette période plutôt qu'aujourd'hui
    const periodeAffichee = ref<Pick<Periode, "dateDebut" | "dateFin"> | null>(
        null
    );

    function ouvrir() {
        aModifier.value = null;
        ouverte.value = true;
    }

    function modifier(operation: Operation, libelle: Libelle | null) {
        aModifier.value = { operation, libelle };
        ouverte.value = true;
    }

    function fermer() {
        ouverte.value = false;
        aModifier.value = null;
    }

    function signalerEnregistrement() {
        nbEnregistrees.value++;
    }

    function afficherPeriode(
        periode: Pick<Periode, "dateDebut" | "dateFin"> | null
    ) {
        periodeAffichee.value = periode;
    }

    // Écran quitté : sa période est oubliée, sauf si l'écran suivant a déjà affiché la sienne
    function oublierPeriode(
        periode: Pick<Periode, "dateDebut" | "dateFin"> | null
    ) {
        if (periode && toRaw(periodeAffichee.value) === toRaw(periode)) {
            periodeAffichee.value = null;
        }
    }

    return {
        ouverte,
        aModifier,
        nbEnregistrees,
        periodeAffichee,
        ouvrir,
        modifier,
        fermer,
        signalerEnregistrement,
        afficherPeriode,
        oublierPeriode,
    };
});
