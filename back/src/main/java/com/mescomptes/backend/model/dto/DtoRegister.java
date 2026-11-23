package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class DtoRegister {
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String nom;
    // Facultatif : un pseudo peut tenir dans le nom seul
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String prenom;
    @NotBlank(message = "L'identifiant est obligatoire.")
    @Pattern(regexp = ReglesSaisie.IDENTIFIANT, message = ReglesSaisie.IDENTIFIANT_MESSAGE)
    private String identifiant;
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Pattern(regexp = ReglesSaisie.MOT_DE_PASSE, message = ReglesSaisie.MOT_DE_PASSE_MESSAGE)
    @OctetsMax(value = ReglesSaisie.MOT_DE_PASSE_OCTETS_MAX, message = ReglesSaisie.MOT_DE_PASSE_OCTETS_MESSAGE)
    @ToString.Exclude
    private String motDePasse;
    @NotBlank(message = "L'adresse mail est obligatoire.")
    @Email(message = "L'adresse mail n'est pas valide.")
    private String email;
}
