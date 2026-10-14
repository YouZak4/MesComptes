package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Total d'une catégorie sur la période, comparé à la période précédente
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoTotalCategorie {
    private Long categorieId;
    private String nom;
    private TypeOperation type;
    private String icone;
    private String couleur;
    private BigDecimal total;
    // Zéro s'il n'y a pas de période précédente
    private BigDecimal totalPrecedent;
    // total - totalPrecedent
    private BigDecimal ecart;
}
