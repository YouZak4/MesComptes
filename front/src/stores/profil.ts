import { defineStore } from "pinia";
import { ref } from "vue";
import { utilisateurService } from "@/services/utilisateurService.ts";
import type { Utilisateur } from "@/types/utilisateur.ts";

// Utilisateur connecté, partagé entre l'en-tête et les réglages du profil
export const useProfilStore = defineStore("profil", () => {
    const utilisateur = ref<Utilisateur | null>(null);

    async function charger(): Promise<Utilisateur> {
        utilisateur.value = await utilisateurService.getMoi();
        return utilisateur.value;
    }

    function mettreAJour(modifie: Utilisateur) {
        utilisateur.value = modifie;
    }

    function vider() {
        utilisateur.value = null;
    }

    return { utilisateur, charger, mettreAJour, vider };
});
