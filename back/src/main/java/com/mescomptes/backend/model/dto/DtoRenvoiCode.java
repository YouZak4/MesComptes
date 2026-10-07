package com.mescomptes.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DtoRenvoiCode {
    @NotBlank(message = "Le jeton d'inscription est obligatoire.")
    private String jeton;
}
