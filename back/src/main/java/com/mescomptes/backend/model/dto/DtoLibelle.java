package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoLibelle {
    private Long id;
    private String nom;
    // Catégorie proposée à la saisie quand on choisit ce libellé
    private Long categorieId;
    // Fourni : commun à tous, non modifiable, seulement masquable
    private boolean fourni;
    private boolean masque;
    private boolean archive;
}
