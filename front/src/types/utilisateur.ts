export interface Utilisateur {
    id: string; // UUID
    nom: string;
    // Facultatif : un pseudo peut tenir dans le nom seul
    prenom: string | null;
    email: string;
}

// Changement d'adresse mail en attente du code envoyé à la nouvelle adresse
export interface ChangementEmail {
    nouvelEmail: string;
    secondesAvantRenvoi: number;
}
