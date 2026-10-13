package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoLogin {
    @NotBlank(message = "L'identifiant est obligatoire.")
    private String identifiant;
    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String motDePasse;
}
