// Montants : le back renvoie des BigDecimal, reçus en JSON comme des nombres
// Dates : chaînes ISO "AAAA-MM-JJ"

export type TypeOperation = "DEPENSE" | "REVENU";

export interface Compte {
    id: number;
    nom: string;
    soldeInitial: number;
    dateSoldeInitial: string;
    archive: boolean;
    solde: number;
    dernierUtilise: boolean;
}

export interface Periode {
    id: number;
    nom: string;
    dateDebut: string;
    dateFin: string;
}

export interface Categorie {
    id: number;
    nom: string;
    type: TypeOperation;
    icone: string | null;
    couleur: string | null;
    fournie: boolean;
    masquee: boolean;
    archive: boolean;
}

export interface Libelle {
    id: number;
    nom: string;
    categorieId: number;
    fourni: boolean;
    masque: boolean;
    archive: boolean;
}

export interface Operation {
    id: number;
    compteId: number;
    date: string;
    type: TypeOperation;
    montant: number;
    categorieId: number;
    libelleId: number | null;
    note: string | null;
    recurrenceId: number | null;
}

export interface Occurrence {
    recurrenceId: number;
    date: string;
    compteId: number;
    type: TypeOperation;
    montant: number;
    categorieId: number;
    libelleId: number | null;
    note: string | null;
}

export interface TotalCategorie {
    categorieId: number;
    nom: string;
    type: TypeOperation;
    icone: string | null;
    couleur: string | null;
    total: number;
    totalPrecedent: number;
    ecart: number;
}

export interface TableauDeBord {
    periode: Periode;
    periodePrecedenteId: number | null;
    periodeSuivanteId: number | null;
    compteId: number | null;
    revenus: number;
    depenses: number;
    solde: number;
    parCategorie: TotalCategorie[];
    aVenir: Occurrence[];
    soldePrevu: number;
    soldeComptesFinPeriode: number;
    dernieresOperations: Operation[];
}
