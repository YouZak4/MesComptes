package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoCompte {
    private Long id;
    private String nom;
    private BigDecimal soldeInitial;
    private LocalDate dateSoldeInitial;
    private boolean archive;
    // Solde initial (au matin de sa date) + revenus - dépenses datés à partir de cette date
    private BigDecimal solde;
    // Compte de la dernière opération saisie : proposé par défaut à la saisie
    private boolean dernierUtilise;
}
