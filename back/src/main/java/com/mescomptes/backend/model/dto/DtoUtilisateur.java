package com.mescomptes.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoUtilisateur {
    private UUID id;
    private String nom;
    private String prenom; // null si non renseigné
    private String email;
    private Set<String> roles; // noms des rôles, ex. ROLE_UTILISATEUR
}
