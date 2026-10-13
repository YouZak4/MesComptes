import api from "./api";
import type { Utilisateur } from "@/types/utilisateur.ts";

export const utilisateurService = {
    async getMoi(): Promise<Utilisateur> {
        const response = await api.get<Utilisateur>("/utilisateurs/moi");
        return response.data;
    },

    // Seul le pseudo se modifie (l'adresse mail demanderait une nouvelle vérification)
    async modifierProfil(pseudo: string): Promise<Utilisateur> {
        const response = await api.put<Utilisateur>("/utilisateurs/moi", {
            pseudo,
        });
        return response.data;
    },

    // Un mot de passe actuel faux répond 422 (et non 401, qui déconnecterait)
    async changerMotDePasse(
        motDePasseActuel: string,
        nouveauMotDePasse: string
    ): Promise<void> {
        await api.put("/utilisateurs/moi/mot-de-passe", {
            motDePasseActuel,
            nouveauMotDePasse,
        });
    },
};
