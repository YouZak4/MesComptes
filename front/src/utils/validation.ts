// Règles de format reprises à l'identique du back (ReglesSaisie.java) : elles valent quand on choisit
// un identifiant ou un mot de passe, jamais à la connexion (un ancien compte doit pouvoir se connecter)

export const REGLE_IDENTIFIANT = /^[a-zA-Z0-9_-]{8,30}$/;
export const MESSAGE_IDENTIFIANT =
    "Entre 8 et 30 caractères, lettres, chiffres, _ et - uniquement.";

// Au moins une majuscule, un chiffre et un caractère qui n'est ni une lettre ni un chiffre
export const REGLE_MOT_DE_PASSE =
    /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/;
export const MESSAGE_MOT_DE_PASSE =
    "8 à 64 caractères, avec au moins une majuscule, un chiffre et un caractère spécial.";

// Le back chiffre avec BCrypt, qui ne lit que 72 octets : une lettre accentuée en compte 2, un symbole jusqu'à 4
export const MOT_DE_PASSE_OCTETS_MAX = 72;
export const MESSAGE_MOT_DE_PASSE_TROP_LONG =
    "Mot de passe trop long : les lettres accentuées et les symboles comptent pour plusieurs caractères.";

// Message d'erreur du mot de passe choisi, ou null s'il est conforme
export function erreurMotDePasse(motDePasse: string): string | null {
    if (!REGLE_MOT_DE_PASSE.test(motDePasse)) {
        return MESSAGE_MOT_DE_PASSE;
    }
    if (new TextEncoder().encode(motDePasse).length > MOT_DE_PASSE_OCTETS_MAX) {
        return MESSAGE_MOT_DE_PASSE_TROP_LONG;
    }
    return null;
}
