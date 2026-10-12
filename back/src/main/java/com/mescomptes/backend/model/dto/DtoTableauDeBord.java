package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord d'une période, pour un compte ou tous.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoTableauDeBord {
    private DtoPeriode periode;
    // Pour les boutons précédente / suivante ; vides aux extrémités
    private Long periodePrecedenteId;
    private Long periodeSuivanteId;
    // Vide : tous les comptes
    private Long compteId;

    // L'équivalent de GAINS, PERTES et BENEFICES de l'Excel
    private BigDecimal revenus;
    private BigDecimal depenses;
    private BigDecimal solde;

    // Trié du plus gros total au plus petit
    private List<DtoTotalCategorie> parCategorie;

    // Occurrences de récurrences pas encore générées dans la période
    private List<DtoOccurrence> aVenir;
    // Solde de la période une fois les occurrences à venir passées
    private BigDecimal soldePrevu;
    // Solde du ou des comptes en fin de période, occurrences à venir comprises
    private BigDecimal soldeComptesFinPeriode;

    private List<DtoOperation> dernieresOperations;
}
