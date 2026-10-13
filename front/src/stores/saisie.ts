import { defineStore } from "pinia";
import { ref } from "vue";

// Fenêtre de saisie rapide, ouverte depuis n'importe quel écran (bouton « + » ou touche N)
export const useSaisieStore = defineStore("saisie", () => {
    const ouverte = ref(false);
    // Incrémenté à chaque opération enregistrée : les écrans le surveillent pour se recharger
    const nbEnregistrees = ref(0);

    function ouvrir() {
        ouverte.value = true;
    }

    function fermer() {
        ouverte.value = false;
    }

    function signalerEnregistrement() {
        nbEnregistrees.value++;
    }

    return { ouverte, nbEnregistrees, ouvrir, fermer, signalerEnregistrement };
});
