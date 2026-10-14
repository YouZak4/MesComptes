package com.mescomptes.backend.controller;

import com.mescomptes.backend.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UtilisateurControllerTests extends ApiTestBase {

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void moiRenvoieLUtilisateurConnecteSansSesSecrets() throws Exception {
        mvc.perform(en(moi, get("/utilisateurs/moi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(moi.getId().toString()))
                .andExpect(jsonPath("$.nom").value(moi.getNom()))
                .andExpect(jsonPath("$.prenom").value(nullValue()))
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

    @Test
    void leNomEtLePrenomSeModifient() throws Exception {
        mvc.perform(en(moi, put("/utilisateurs/moi")).content("""
                        {"nom": "  Dupont  ", "prenom": " Jean "}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.prenom").value("Jean"));

        // Prénom vide : retiré (un pseudo tient dans le nom seul)
        mvc.perform(en(moi, put("/utilisateurs/moi")).content("""
                        {"nom": "Youzaka", "prenom": "  "}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value(nullValue()));
    }

    @Test
    void deuxUtilisateursPeuventPorterLeMemeNom() throws Exception {
        mvc.perform(en(moi, put("/utilisateurs/moi")).content("""
                        {"nom": "%s"}""".formatted(autre.getNom())))
                .andExpect(status().isOk());
    }

    @Test
    void leNomEstObligatoire() throws Exception {
        mvc.perform(en(moi, put("/utilisateurs/moi")).content("""
                        {"nom": "  ", "prenom": "Jean"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leMotDePasseChangeSeulementAvecLActuel() throws Exception {
        moi.setMotDePasse(passwordEncoder.encode("ancien"));
        utilisateurRepository.save(moi);

        // 422 et non 401 : le front déconnecte sur 401
        mvc.perform(en(moi, put("/utilisateurs/moi/mot-de-passe")).content("""
                        {"motDePasseActuel": "faux", "nouveauMotDePasse": "nouveau"}"""))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, put("/utilisateurs/moi/mot-de-passe")).content("""
                        {"motDePasseActuel": "ancien", "nouveauMotDePasse": "nouveau"}"""))
                .andExpect(status().isNoContent());

        assertThat(passwordEncoder.matches("nouveau", utilisateurRepository.findById(moi.getId()).orElseThrow().getMotDePasse())).isTrue();
    }

    // ── Changement d'adresse mail ──

    private void definirMotDePasse() {
        moi.setMotDePasse(passwordEncoder.encode("secret"));
        utilisateurRepository.save(moi);
    }

    private String nouvelleAdresse() {
        return "nouvelle-" + moi.getId() + "@mescomptes.local";
    }

    // Demande le changement et renvoie le code « envoyé » à la nouvelle adresse
    private String demanderChangement(String email) throws Exception {
        mvc.perform(en(moi, put("/utilisateurs/moi/email")).content("""
                        {"nouvelEmail": "%s", "motDePasse": "secret"}""".formatted(email.toUpperCase())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.nouvelEmail").value(email))
                .andExpect(jsonPath("$.secondesAvantRenvoi").isNumber());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailService).envoyerCodeChangementEmail(eq(email), code.capture());
        return code.getValue();
    }

    @Test
    void lAdresseMailChangeUneFoisLeCodeSaisi() throws Exception {
        definirMotDePasse();
        String ancienne = moi.getEmail();
        String code = demanderChangement(nouvelleAdresse());

        // Tant que le code n'est pas saisi, l'adresse ne change pas
        mvc.perform(en(moi, get("/utilisateurs/moi")))
                .andExpect(jsonPath("$.email").value(ancienne));
        mvc.perform(en(moi, get("/utilisateurs/moi/email")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouvelEmail").value(nouvelleAdresse()));

        mvc.perform(en(moi, post("/utilisateurs/moi/email/verification")).content("""
                        {"code": "%s"}""".formatted(code.equals("000000") ? "000001" : "000000")))
                .andExpect(status().isBadRequest());
        mvc.perform(en(moi, post("/utilisateurs/moi/email/verification")).content("""
                        {"code": "%s"}""".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(nouvelleAdresse()));

        // L'ancienne adresse est prévenue, et plus rien n'est en attente
        verify(mailService).envoyerAvisChangementEmail(ancienne, nouvelleAdresse());
        mvc.perform(en(moi, get("/utilisateurs/moi/email")))
                .andExpect(status().isNoContent());
    }

    @Test
    void leChangementDAdresseExigeLeMotDePasse() throws Exception {
        definirMotDePasse();
        // 422 et non 401 : le front déconnecte sur 401
        mvc.perform(en(moi, put("/utilisateurs/moi/email")).content("""
                        {"nouvelEmail": "%s", "motDePasse": "faux"}""".formatted(nouvelleAdresse())))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, put("/utilisateurs/moi/email")).content("""
                        {"nouvelEmail": "%s", "motDePasse": "secret"}""".formatted(moi.getEmail())))
                .andExpect(status().isUnprocessableContent());
        verify(mailService, never()).envoyerCodeChangementEmail(anyString(), anyString());
    }

    @Test
    void uneAdresseDejaPriseNeLeDitPasMaisNeChangeRien() throws Exception {
        definirMotDePasse();
        // Même réponse qu'une adresse libre : on ne peut pas tester qui est inscrit
        mvc.perform(en(moi, put("/utilisateurs/moi/email")).content("""
                        {"nouvelEmail": "%s", "motDePasse": "secret"}""".formatted(autre.getEmail())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.nouvelEmail").value(autre.getEmail()));

        // Aucun code n'est envoyé ; le titulaire de l'adresse est prévenu
        verify(mailService, never()).envoyerCodeChangementEmail(anyString(), anyString());
        verify(mailService).envoyerAlerteEmailDejaUtilise(autre.getEmail());
    }

    @Test
    void uneNouvelleDemandeAttendLeDelaiDeRenvoi() throws Exception {
        definirMotDePasse();
        demanderChangement(nouvelleAdresse());

        mvc.perform(en(moi, post("/utilisateurs/moi/email/renvoi")))
                .andExpect(status().isTooManyRequests());
        mvc.perform(en(moi, put("/utilisateurs/moi/email")).content("""
                        {"nouvelEmail": "autre-%s", "motDePasse": "secret"}""".formatted(nouvelleAdresse())))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unChangementAnnuleNeSeValidePlus() throws Exception {
        definirMotDePasse();
        String code = demanderChangement(nouvelleAdresse());

        mvc.perform(en(moi, delete("/utilisateurs/moi/email")))
                .andExpect(status().isNoContent());
        mvc.perform(en(moi, post("/utilisateurs/moi/email/verification")).content("""
                        {"code": "%s"}""".formatted(code)))
                .andExpect(status().isBadRequest());
        mvc.perform(en(moi, get("/utilisateurs/moi")))
                .andExpect(jsonPath("$.email").value(moi.getEmail()));
    }

    @Test
    void leChangementDUnAutreNestPasAccessible() throws Exception {
        definirMotDePasse();
        String code = demanderChangement(nouvelleAdresse());

        mvc.perform(en(autre, get("/utilisateurs/moi/email")))
                .andExpect(status().isNoContent());
        mvc.perform(en(autre, post("/utilisateurs/moi/email/verification")).content("""
                        {"code": "%s"}""".formatted(code)))
                .andExpect(status().isBadRequest());
    }
}
