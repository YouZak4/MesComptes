package com.mescomptes.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règles de format de l'identifiant et du mot de passe à l'inscription, et connexion d'un compte
 * dont le mot de passe ne les suit pas (créé avant elles).
 */
class InscriptionTests extends ApiTestBase {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String inscription(String identifiant, String motDePasse) {
        String email = "inscription-" + UUID.randomUUID().toString().substring(0, 8) + "@mescomptes.local";
        return """
                {"nom": "Dupont", "identifiant": "%s", "motDePasse": "%s", "email": "%s"}"""
                .formatted(identifiant, motDePasse, email);
    }

    private String identifiantNeuf() {
        return "insc-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void uneInscriptionConformeAttendSonCode() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(inscription(identifiantNeuf(), "Correct-123")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jeton").isNotEmpty());
    }

    @Test
    void unMotDePasseLongEstAccepteJusquALaLimite() throws Exception {
        // 64 caractères sans accent, puis 71 octets avec des lettres accentuées (2 octets chacune)
        for (String accepte : new String[]{"A1-" + "a".repeat(61), "A1-" + "é".repeat(34)}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(inscription(identifiantNeuf(), accepte)))
                    .andExpect(status().isAccepted());
        }
    }

    @Test
    void unMotDePasseDePlusDe72OctetsEstRefuse() throws Exception {
        // 38 caractères seulement, mais 73 octets : BCrypt en ignorerait la fin
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(inscription(identifiantNeuf(), "A1-" + "é".repeat(35))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("trop long")));
        verifyNoInteractions(mailService);
    }

    @Test
    void uneConnexionAvecUnMotDePasseTresLongEchoueSansErreurServeur() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifiant": "%s", "motDePasse": "%s"}"""
                                .formatted(moi.getIdentifiant(), "A1-" + "é".repeat(200))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unMotDePasseHorsRegleEstRefuseSansEnvoyerDeMail() throws Exception {
        for (String refuse : new String[]{"court", "Court-1", "sansmajuscule-1", "SansChiffre-x", "SansSpecial1",
                "A1-" + "a".repeat(62)}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(inscription(identifiantNeuf(), refuse)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("majuscule, un chiffre et un caractère spécial")));
        }
        verifyNoInteractions(mailService);
    }

    @Test
    void unIdentifiantHorsRegleEstRefuse() throws Exception {
        for (String refuse : new String[]{"court", "avec espace", "accentué-01", "identifiant-beaucoup-trop-long-31"}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(inscription(refuse, "Correct-123")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(containsString("entre 8 et 30 caractères")));
        }
        verifyNoInteractions(mailService);
    }

    @Test
    void unCompteAuMotDePasseHorsRegleSeConnecteToujours() throws Exception {
        moi.setMotDePasse(passwordEncoder.encode("abc"));
        utilisateurRepository.save(moi);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifiant": "%s", "motDePasse": "abc"}""".formatted(moi.getIdentifiant())))
                .andExpect(status().isOk());
    }
}
