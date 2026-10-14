package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoPeriode {
    private Long id;
    private String nom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
