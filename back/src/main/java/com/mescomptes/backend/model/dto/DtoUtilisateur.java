package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoUtilisateur {
    private Long id;
    private String nom;
    private String prenom; // null si non renseigné
    private String email;
    private Set<String> roles; // noms des rôles, ex. ROLE_UTILISATEUR
}
