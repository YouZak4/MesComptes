package com.mescomptes.backend.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jeton absent ou refusé : 401 au format DtoApiError, jamais une erreur 500 ni un 403 sans corps.
 * Ici les requêtes portent un vrai en-tête Authorization (pas l'utilisateur injecté par en()).
 */
class SecuriteTests extends ApiTestBase {

    private static final long UNE_HEURE = 3_600_000;

    @Value("${jwt.secret}")
    private String secret;

    private String jeton(String identifiant, long decalageExpiration, String cle) {
        return Jwts.builder()
                .subject(identifiant)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + decalageExpiration))
                .signWith(Keys.hmacShaKeyFor(cle.getBytes()))
                .compact();
    }

    @Test
    void unJetonValideDonneAcces() throws Exception {
        mvc.perform(get("/utilisateurs/moi").header("Authorization", "Bearer " + jeton(moi.getIdentifiant(), UNE_HEURE, secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(moi.getEmail()));
    }

    @Test
    void sansJetonUneRouteProtegeeRepond401() throws Exception {
        mvc.perform(get("/comptes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("AUTH_REQUIRED"));
    }

    @Test
    void unJetonExpireRepond401() throws Exception {
        mvc.perform(get("/comptes").header("Authorization", "Bearer " + jeton(moi.getIdentifiant(), -UNE_HEURE, secret)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    @Test
    void unJetonMalformeOuMalSigneRepond401() throws Exception {
        mvc.perform(get("/comptes").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
        // Signé avec une autre clé (secret changé depuis l'émission)
        String autreCle = "une-autre-cle-de-test-assez-longue-pour-hmac-sha";
        mvc.perform(get("/comptes").header("Authorization", "Bearer " + jeton(moi.getIdentifiant(), UNE_HEURE, autreCle)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void unJetonSansIdentifiantRepond401() throws Exception {
        String sansSujet = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + UNE_HEURE))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
        mvc.perform(get("/comptes").header("Authorization", "Bearer " + sansSujet))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void leJetonDUnUtilisateurQuiNExistePlusRepond401() throws Exception {
        mvc.perform(get("/comptes").header("Authorization", "Bearer " + jeton("identifiant-inexistant", UNE_HEURE, secret)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void unJetonRefuseNeBloquePasUneRoutePublique() throws Exception {
        // La connexion répond selon les identifiants saisis, pas selon le vieux jeton envoyé
        mvc.perform(post("/auth/login").header("Authorization", "Bearer abc.def.ghi")
                        .contentType("application/json")
                        .content("""
                                {"identifiant": "identifiant-inexistant", "motDePasse": "faux"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH_FAILED"));
    }
}
