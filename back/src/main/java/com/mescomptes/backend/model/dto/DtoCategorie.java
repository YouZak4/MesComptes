package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.enums.TypeOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoCategorie {
    private Long id;
    private String nom;
    private TypeOperation type;
    private String icone;
    private String couleur;
    // Fournie : commune à tous, non modifiable, seulement masquable
    private boolean fournie;
    private boolean masquee;
    private boolean archive;
}
