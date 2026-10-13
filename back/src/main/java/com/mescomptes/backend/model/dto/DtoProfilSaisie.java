package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Modification du profil : seul le pseudo change ici (l'adresse mail demanderait une nouvelle vérification)
@Data
public class DtoProfilSaisie {
    @NotBlank(message = "Le pseudonyme est obligatoire.")
    @Size(max = 255, message = "Le pseudonyme ne doit pas dépasser 255 caractères.")
    private String pseudo;
}
