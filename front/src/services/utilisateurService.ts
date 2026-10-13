import api from "./api";
import type { Utilisateur } from "@/types/utilisateur.ts";

export const utilisateurService = {
    async getMoi(): Promise<Utilisateur> {
        const response = await api.get<Utilisateur>("/utilisateurs/moi");
        return response.data;
    },
};
