package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoCodeSaisie {
    @NotBlank(message = "Le code de vérification est obligatoire.")
    private String code;
}
