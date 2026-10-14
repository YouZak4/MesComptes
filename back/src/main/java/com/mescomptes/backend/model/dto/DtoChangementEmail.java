package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Changement d'adresse en attente de son code. L'adresse est celle que l'utilisateur vient de saisir :
 * la lui renvoyer en entier ne révèle rien.
 */
@Data
@AllArgsConstructor
public class DtoChangementEmail {
    private String nouvelEmail;
    // Permet au front d'afficher le bon décompte, même après un rafraîchissement
    private long secondesAvantRenvoi;
}
