package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Modification du profil : nom et prénom (l'adresse mail a sa propre route, avec vérification par code)
@Data
public class DtoProfilSaisie {
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String nom;
    // Facultatif ; vide ou blanc : retiré
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String prenom;
}
