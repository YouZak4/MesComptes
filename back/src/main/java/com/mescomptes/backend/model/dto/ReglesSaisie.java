package com.mescomptes.backend.model.dto;

/**
 * Règles de format communes à plusieurs DTO de saisie, reprises à l'identique par le front
 * (`front/src/utils/validation.ts`). Elles s'appliquent quand on choisit un identifiant ou un mot de passe,
 * jamais à la connexion : un compte créé avant ces règles doit pouvoir se connecter.
 */
public final class ReglesSaisie {

    public static final String IDENTIFIANT = "^[a-zA-Z0-9_-]{8,30}$";
    public static final String IDENTIFIANT_MESSAGE =
            "L'identifiant doit faire entre 8 et 30 caractères : lettres, chiffres, _ et - uniquement.";

    // Au moins une majuscule, un chiffre et un caractère qui n'est ni une lettre ni un chiffre
    public static final String MOT_DE_PASSE = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,30}$";
    public static final String MOT_DE_PASSE_MESSAGE =
            "Le mot de passe doit faire entre 8 et 30 caractères, avec au moins une majuscule, un chiffre et un caractère spécial.";

    private ReglesSaisie() {
    }
}
