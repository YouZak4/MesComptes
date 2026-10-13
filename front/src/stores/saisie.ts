import { defineStore } from "pinia";
import { ref } from "vue";
import type { Libelle, Operation } from "@/types/comptes.ts";

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

    return {
        ouverte,
        aModifier,
        nbEnregistrees,
        ouvrir,
        modifier,
        fermer,
        signalerEnregistrement,
    };
});
