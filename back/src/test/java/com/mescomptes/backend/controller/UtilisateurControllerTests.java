package com.mescomptes.backend.controller;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UtilisateurControllerTests extends ApiTestBase {

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
