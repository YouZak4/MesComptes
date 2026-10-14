import api from "./api";
import type { ChangementEmail, Utilisateur } from "@/types/utilisateur.ts";

export const utilisateurService = {
    async getMoi(): Promise<Utilisateur> {
        const response = await api.get<Utilisateur>("/utilisateurs/moi");
        return response.data;
    },

    // Prénom vide : retiré
    async modifierProfil(nom: string, prenom: string): Promise<Utilisateur> {
        const response = await api.put<Utilisateur>("/utilisateurs/moi", {
            nom,
            prenom,
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

    // ── Changement d'adresse mail : un code est envoyé à la nouvelle adresse ──

    // null : aucun changement en cours (204)
    async getChangementEmail(): Promise<ChangementEmail | null> {
        const response = await api.get<ChangementEmail>(
            "/utilisateurs/moi/email"
        );
        return response.status === 204 ? null : response.data;
    },

    async demanderChangementEmail(
        nouvelEmail: string,
        motDePasse: string
    ): Promise<ChangementEmail> {
        const response = await api.put<ChangementEmail>(
            "/utilisateurs/moi/email",
            { nouvelEmail, motDePasse }
        );
        return response.data;
    },

    async verifierChangementEmail(code: string): Promise<Utilisateur> {
        const response = await api.post<Utilisateur>(
            "/utilisateurs/moi/email/verification",
            { code }
        );
        return response.data;
    },

    async renvoyerCodeChangementEmail(): Promise<ChangementEmail> {
        const response = await api.post<ChangementEmail>(
            "/utilisateurs/moi/email/renvoi"
        );
        return response.data;
    },

    async annulerChangementEmail(): Promise<void> {
        await api.delete("/utilisateurs/moi/email");
    },
};
