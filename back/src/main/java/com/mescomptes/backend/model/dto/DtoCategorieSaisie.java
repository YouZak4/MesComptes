package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.TypeOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Création et modification d'une catégorie personnelle
@Data
public class DtoCategorieSaisie {
    @NotBlank(message = "Le nom de la catégorie est obligatoire.")
    @Size(max = 50, message = "Le nom de la catégorie ne doit pas dépasser 50 caractères.")
    private String nom;
    @NotNull(message = "Le type de la catégorie (dépense ou revenu) est obligatoire.")
    private TypeOperation type;
    @Size(max = 16, message = "L'icône ne doit pas dépasser 16 caractères.")
    private String icone;
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit être au format #RRGGBB.")
    private String couleur;
    // Ignoré à la création. En modification : true archive, false désarchive, absent (null) garde l'état actuel
    private Boolean archive;
}
