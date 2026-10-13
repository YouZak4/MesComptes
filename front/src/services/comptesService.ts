import api from "./api";
import type {
    Categorie,
    Compte,
    FiltreOperations,
    Frequence,
    Libelle,
    Operation,
    OperationSaisie,
    Periode,
    Recurrence,
    RecurrenceSaisie,
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

    async getPeriodes(): Promise<Periode[]> {
        const response = await api.get<Periode[]>("/periodes");
        return response.data;
    },

    // Opérations les plus récentes d'abord ; chaque filtre est facultatif
    async getOperations(filtre: FiltreOperations): Promise<Operation[]> {
        const response = await api.get<Operation[]>("/operations", {
            params: filtre,
        });
        return response.data;
    },

    async modifierOperation(
        id: number,
        saisie: OperationSaisie
    ): Promise<Operation> {
        const response = await api.put<Operation>(`/operations/${id}`, saisie);
        return response.data;
    },

    async supprimerOperation(id: number): Promise<void> {
        await api.delete(`/operations/${id}`);
    },

    // « Répéter chaque mois » : l'opération devient la première occurrence d'une récurrence
    async repeterOperation(
        id: number,
        frequence: Frequence = "MENSUELLE"
    ): Promise<void> {
        await api.post(`/operations/${id}/repeter`, null, {
            params: { frequence },
        });
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

    // Arrêtées comprises, triées par jour du mois
    async getRecurrences(): Promise<Recurrence[]> {
        const response = await api.get<Recurrence[]>("/recurrences");
        return response.data;
    },

    // Les opérations déjà échues depuis la date de début sont créées tout de suite
    async creerRecurrence(saisie: RecurrenceSaisie): Promise<Recurrence> {
        const response = await api.post<Recurrence>("/recurrences", saisie);
        return response.data;
    },

    // Ne touche que les occurrences futures
    async modifierRecurrence(
        id: number,
        saisie: RecurrenceSaisie
    ): Promise<Recurrence> {
        const response = await api.put<Recurrence>(
            `/recurrences/${id}`,
            saisie
        );
        return response.data;
    },

    // La récurrence arrêtée (l'historique reste), ou null si elle n'avait rien généré et a été supprimée
    async arreterRecurrence(id: number): Promise<Recurrence | null> {
        const response = await api.delete<Recurrence>(`/recurrences/${id}`);
        return response.status === 204 ? null : response.data;
    },
};
