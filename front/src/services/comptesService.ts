import api from "./api";
import type {
    Categorie,
    Compte,
    Libelle,
    Operation,
    OperationSaisie,
    Periode,
    TableauDeBord,
} from "@/types/comptes.ts";

export const comptesService = {
    async getComptes(): Promise<Compte[]> {
        const response = await api.get<Compte[]>("/comptes");
        return response.data;
    },

    async getCategories(): Promise<Categorie[]> {
        const response = await api.get<Categorie[]>("/categories");
        return response.data;
    },

    async getLibelles(): Promise<Libelle[]> {
        const response = await api.get<Libelle[]>("/libelles");
        return response.data;
    },

    // Autocomplétion : les libellés visibles contenant le texte, les plus utilisés d'abord
    async rechercherLibelles(texte: string): Promise<Libelle[]> {
        const response = await api.get<Libelle[]>("/libelles", {
            params: { q: texte },
        });
        return response.data;
    },

    // Libellé personnel créé à la volée depuis la saisie
    async creerLibelle(nom: string, categorieId: number): Promise<Libelle> {
        const response = await api.post<Libelle>("/libelles", {
            nom,
            categorieId,
        });
        return response.data;
    },

    async creerOperation(saisie: OperationSaisie): Promise<Operation> {
        const response = await api.post<Operation>("/operations", saisie);
        return response.data;
    },

    async creerPeriode(dateDebut: string, dateFin: string): Promise<Periode> {
        const response = await api.post<Periode>("/periodes", {
            dateDebut,
            dateFin,
        });
        return response.data;
    },

    // Sans période : celle qui contient aujourd'hui, sinon la dernière. Sans compte : tous les comptes
    async getTableauDeBord(
        periodeId?: number,
        compteId?: number
    ): Promise<TableauDeBord> {
        const response = await api.get<TableauDeBord>("/tableau-de-bord", {
            params: { periodeId, compteId },
        });
        return response.data;
    },
};
