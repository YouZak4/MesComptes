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
    // Solde initial + revenus - dépenses datés après la date du solde initial
    private BigDecimal solde;
}
