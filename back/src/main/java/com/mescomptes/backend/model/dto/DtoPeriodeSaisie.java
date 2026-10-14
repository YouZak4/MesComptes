package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Création et modification d'une période.
 * Nom vide : nom par défaut (mois de la date de fin).
 * À la création, les dates manquantes sont déduites des périodes existantes (voir PeriodeService).
 */
@Data
public class DtoPeriodeSaisie {
    @Size(max = 50, message = "Le nom de la période ne doit pas dépasser 50 caractères.")
    private String nom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
