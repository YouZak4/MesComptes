package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

// Changement de mot de passe : l'actuel est redemandé, au cas où la session serait restée ouverte
@Data
public class DtoMotDePasseSaisie {
    @NotBlank(message = "Le mot de passe actuel est obligatoire.")
    @ToString.Exclude
    private String motDePasseActuel;
    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Pattern(regexp = ReglesSaisie.MOT_DE_PASSE, message = ReglesSaisie.MOT_DE_PASSE_MESSAGE)
    @OctetsMax(value = ReglesSaisie.MOT_DE_PASSE_OCTETS_MAX, message = ReglesSaisie.MOT_DE_PASSE_OCTETS_MESSAGE)
    @ToString.Exclude
    private String nouveauMotDePasse;
}
