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

export interface OperationSaisie {
    compteId: number;
    date: string;
    montant: number;
    categorieId: number;
    libelleId: number | null;
    note: string | null;
}

export type Frequence = "MENSUELLE" | "ANNUELLE";

export interface FiltreOperations {
    periodeId?: number;
    compteId?: number;
    categorieId?: number;
    libelleId?: number;
    q?: string;
}

export interface Recurrence {
    id: number;
    compteId: number;
    type: TypeOperation;
    montant: number;
    categorieId: number;
    libelleId: number | null;
    note: string | null;
    frequence: Frequence;
    // 1 à 31 ; un jour absent du mois devient le dernier jour du mois
    jour: number;
    dateDebut: string;
    dateFin: string | null;
    prochaineOccurrence: string;
    // Faux une fois la date de fin dépassée (récurrence arrêtée, prêt remboursé)
    active: boolean;
}

// Une récurrence annuelle tombe chaque année dans le mois de sa date de début
export interface RecurrenceSaisie {
    compteId: number;
    montant: number;
    categorieId: number;
    libelleId: number | null;
    note: string | null;
    frequence: Frequence;
    jour: number;
    dateDebut: string;
    dateFin: string | null;
}

// archive : ignoré à la création ; false désarchive un compte
export interface CompteSaisie {
    nom: string;
    soldeInitial: number;
    dateSoldeInitial: string;
    archive: boolean;
}

// Nom vide : mois de la date de fin
export interface PeriodeSaisie {
    nom: string | null;
    dateDebut: string;
    dateFin: string;
}

export interface CategorieSaisie {
    nom: string;
    type: TypeOperation;
    icone: string | null;
    couleur: string | null;
    archive: boolean;
}

export interface LibelleSaisie {
    nom: string;
    categorieId: number;
    archive: boolean;
}

// Éléments fournis (communs à tous) que l'utilisateur peut masquer
export type TypeMasquable = "categorie" | "libelle";
