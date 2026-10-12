package com.mescomptes.backend.model;

import java.util.Locale;

public final class Noms {

    private Noms() {
    }

    /**
     * Supprime les espaces en trop : "  grande   surface " devient "grande surface".
     */
    public static String nettoyer(String nom) {
        return nom.trim().replaceAll("\\s+", " ");
    }

    /**
     * Forme utilisée pour l'unicité des noms, sans tenir compte de la casse ni des espaces en trop.
     */
    public static String normaliser(String nom) {
        return nettoyer(nom).toLowerCase(Locale.ROOT);
    }
}
