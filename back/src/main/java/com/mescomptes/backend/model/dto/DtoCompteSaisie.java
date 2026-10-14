package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// Création et modification d'un compte
@Data
public class DtoCompteSaisie {
    @NotBlank(message = "Le nom du compte est obligatoire.")
    @Size(max = 100, message = "Le nom du compte ne doit pas dépasser 100 caractères.")
    private String nom;
    @NotNull(message = "Le solde initial est obligatoire.")
    @Digits(integer = 10, fraction = 2, message = "Le solde initial doit avoir au plus 2 décimales.")
    private BigDecimal soldeInitial;
    @NotNull(message = "La date du solde initial est obligatoire.")
    private LocalDate dateSoldeInitial;
    // Ignoré à la création. En modification : true archive, false désarchive, absent (null) garde l'état actuel
    private Boolean archive;
}
