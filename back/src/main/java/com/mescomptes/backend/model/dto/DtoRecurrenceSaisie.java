package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.Frequence;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Création et modification d'une récurrence : les champs d'une opération, plus le calendrier.
 * Le type (dépense ou revenu) est déduit de la catégorie.
 */
@Data
public class DtoRecurrenceSaisie {
    @NotNull(message = "Le compte est obligatoire.")
    private Long compteId;
    @NotNull(message = "Le montant est obligatoire.")
    @Positive(message = "Le montant doit être positif ; le type de la catégorie donne le sens.")
    @DecimalMax(value = "9999999999.99", message = "Le montant est trop grand.")
    private BigDecimal montant;
    // Vide : catégorie par défaut du libellé (l'un des deux est obligatoire)
    private Long categorieId;
    private Long libelleId;
    @Size(max = 255, message = "La note ne doit pas dépasser 255 caractères.")
    private String note;
    @NotNull(message = "La fréquence est obligatoire.")
    private Frequence frequence;
    // Vide : jour de la date de début
    @Min(value = 1, message = "Le jour doit être compris entre 1 et 31.")
    @Max(value = 31, message = "Le jour doit être compris entre 1 et 31.")
    private Integer jour;
    @NotNull(message = "La date de début est obligatoire.")
    private LocalDate dateDebut;
    // Facultative (fin d'un prêt)
    private LocalDate dateFin;
}
