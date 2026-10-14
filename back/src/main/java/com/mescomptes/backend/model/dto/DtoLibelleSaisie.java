package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Création et modification d'un libellé personnel
@Data
public class DtoLibelleSaisie {
    @NotBlank(message = "Le nom du libellé est obligatoire.")
    @Size(max = 100, message = "Le nom du libellé ne doit pas dépasser 100 caractères.")
    private String nom;
    @NotNull(message = "La catégorie par défaut du libellé est obligatoire.")
    private Long categorieId;
    // Ignoré à la création. En modification : true archive, false désarchive, absent (null) garde l'état actuel
    private Boolean archive;
}
