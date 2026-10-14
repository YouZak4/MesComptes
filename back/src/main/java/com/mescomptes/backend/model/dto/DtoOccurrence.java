package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Occurrence d'une récurrence pas encore générée
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoOccurrence {
    private Long recurrenceId;
    private LocalDate date;
    private Long compteId;
    private TypeOperation type;
    private BigDecimal montant;
    private Long categorieId;
    private Long libelleId;
    private String note;
}
