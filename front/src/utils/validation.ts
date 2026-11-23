// Règles de format reprises à l'identique du back (ReglesSaisie.java) : elles valent quand on choisit
// un identifiant ou un mot de passe, jamais à la connexion (un ancien compte doit pouvoir se connecter)

export const REGLE_IDENTIFIANT = /^[a-zA-Z0-9_-]{8,30}$/;
export const MESSAGE_IDENTIFIANT =
    "Entre 8 et 30 caractères, lettres, chiffres, _ et - uniquement.";

// Au moins une majuscule, un chiffre et un caractère qui n'est ni une lettre ni un chiffre
export const REGLE_MOT_DE_PASSE =
    /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,30}$/;
export const MESSAGE_MOT_DE_PASSE =
    "8 à 30 caractères, avec au moins une majuscule, un chiffre et un caractère spécial.";

export function motDePasseConforme(motDePasse: string): boolean {
    return REGLE_MOT_DE_PASSE.test(motDePasse);
}
