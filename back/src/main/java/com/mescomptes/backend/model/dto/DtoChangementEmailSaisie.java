package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

// Demande de changement d'adresse : le mot de passe protège une session laissée ouverte
@Data
public class DtoChangementEmailSaisie {
    @NotBlank(message = "La nouvelle adresse mail est obligatoire.")
    @Email(message = "L'adresse mail n'est pas valide.")
    private String nouvelEmail;
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @ToString.Exclude
    private String motDePasse;
}
