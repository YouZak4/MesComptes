package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Création et modification d'une opération.
 * Le type (dépense ou revenu) n'est pas saisi : il est déduit de la catégorie.
 */
@Data
public class DtoOperationSaisie {
    @NotNull(message = "Le compte est obligatoire.")
    private Long compteId;
    // Vide : aujourd'hui
    private LocalDate date;
    // Arrondi au centime à l'enregistrement
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être positif ; le type de la catégorie donne le sens.")
    @DecimalMax(value = "9999999999.99", message = "Le montant est trop grand.")
    private BigDecimal montant;
    // Vide : catégorie par défaut du libellé (l'un des deux est obligatoire)
    private Long categorieId;
    private Long libelleId;
    @Size(max = 255, message = "La note ne doit pas dépasser 255 caractères.")
    private String note;
}
