package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoOperation {
    private Long id;
    private Long compteId;
    private LocalDate date;
    private TypeOperation type;
    // Toujours positif : le type donne le sens
    private BigDecimal montant;
    private Long categorieId;
    private Long libelleId;
    private String note;
    // Renseigné si l'opération a été générée par une récurrence
    private Long recurrenceId;
}
