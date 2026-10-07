package com.mescomptes.backend.model.dto;

import com.mescomptes.backend.model.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DtoUtilisateur {
    private Long id;
    private String pseudo;
    private String email;
    private Set<Role> roles;
}
