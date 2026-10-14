package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoRecurrence {
    private Long id;
    private Long compteId;
    private TypeOperation type;
    private BigDecimal montant;
    private Long categorieId;
    private Long libelleId;
    private String note;
    private Frequence frequence;
    private int jour;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate prochaineOccurrence;
    // Faux une fois la date de fin dépassée (prêt remboursé, récurrence arrêtée)
    private boolean active;
}
