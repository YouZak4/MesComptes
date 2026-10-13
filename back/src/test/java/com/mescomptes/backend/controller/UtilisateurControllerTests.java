package com.mescomptes.backend.controller;

import com.mescomptes.backend.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UtilisateurControllerTests extends ApiTestBase {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void moiRenvoieLUtilisateurConnecteSansSesSecrets() throws Exception {
        mvc.perform(en(moi, get("/utilisateurs/moi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(moi.getId()))
                .andExpect(jsonPath("$.pseudo").value(moi.getPseudo()))
                .andExpect(jsonPath("$.email").value(moi.getEmail()))
                .andExpect(jsonPath("$", not(hasKey("motDePasse"))))
                .andExpect(jsonPath("$", not(hasKey("identifiant"))));
    }

    @Test
    void lesRolesSontExposesParLeurNom() throws Exception {
        moi.getRoles().add(roleRepository.findByNom("ROLE_UTILISATEUR").orElseThrow());
        utilisateurRepository.save(moi);

        mvc.perform(en(moi, get("/utilisateurs/moi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(contains("ROLE_UTILISATEUR")));
    }

    @Test
    void laListeDesUtilisateursNestPlusExposee() throws Exception {
        mvc.perform(en(moi, get("/utilisateurs")))
                .andExpect(status().is4xxClientError());
        mvc.perform(en(moi, get("/api/utilisateurs")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void moiExigeDEtreConnecte() throws Exception {
        mvc.perform(get("/utilisateurs/moi"))
                .andExpect(status().is4xxClientError());
    }
}
