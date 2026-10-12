package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Periode;
import com.mescomptes.backend.repository.PeriodeRepository;
import com.mescomptes.backend.service.PeriodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompteEtPeriodeControllerTests extends ApiTestBase {

    @Autowired
    private PeriodeRepository periodeRepository;
    @Autowired
    private PeriodeService periodeService;

    // ----- Comptes -----

    @Test
    void creerPuisListerUnCompteAvecSonSolde() throws Exception {
        mvc.perform(en(moi, post("/comptes")).content("""
                        {"nom": "  Livret   A ", "soldeInitial": 1000.50, "dateSoldeInitial": "2026-01-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Livret A"))
                .andExpect(jsonPath("$.solde").value(1000.50));

        Compte compte = compteRepository.findByUtilisateurIdOrderByNom(moi.getId()).getFirst();
        Categorie salaire = categorieFournie("Salaire");
        Categorie courses = categorieFournie("Courses");
        operation(compte, salaire, null, "2026-02-01", "200.00");
        operation(compte, courses, null, "2026-02-02", "50.25");
        // Datée le jour du solde initial : déjà comprise dans celui-ci
        operation(compte, courses, null, "2026-01-01", "999.00");

        mvc.perform(en(moi, get("/comptes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].solde").value(1150.25));
    }

    @Test
    void refuserUnCompteSansNom() throws Exception {
        mvc.perform(en(moi, post("/comptes")).content("""
                        {"nom": " ", "soldeInitial": 0, "dateSoldeInitial": "2026-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void supprimerUnCompteVideEtArchiverUnCompteUtilise() throws Exception {
        Compte vide = compte(moi, "Vide");
        Compte utilise = compte(moi, "Utilisé");
        operation(utilise, categorieFournie("Courses"), null, "2026-03-01", "10.00");

        mvc.perform(en(moi, delete("/comptes/" + vide.getId()))).andExpect(status().isNoContent());
        mvc.perform(en(moi, delete("/comptes/" + utilise.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive").value(true));

        assertThat(compteRepository.findById(vide.getId())).isEmpty();
        assertThat(compteRepository.findById(utilise.getId())).isPresent();
    }

    @Test
    void neJamaisExposerLeCompteDUnAutreUtilisateur() throws Exception {
        Compte compteAutre = compte(autre, "Secret");

        mvc.perform(en(moi, put("/comptes/" + compteAutre.getId())).content("""
                        {"nom": "Piraté", "soldeInitial": 0, "dateSoldeInitial": "2026-01-01"}"""))
                .andExpect(status().isNotFound());
        mvc.perform(en(moi, delete("/comptes/" + compteAutre.getId()))).andExpect(status().isNotFound());
        mvc.perform(en(moi, get("/comptes"))).andExpect(jsonPath("$.length()").value(0));
    }

    // ----- Périodes -----

    @Test
    void creerLaPremierePeriodePuisLaSuivanteParDefaut() throws Exception {
        mvc.perform(en(moi, post("/periodes")).content("""
                        {"dateDebut": "2026-01-23", "dateFin": "2026-02-25"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Février 2026"));

        mvc.perform(en(moi, post("/periodes")).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateDebut").value("2026-02-26"))
                .andExpect(jsonPath("$.dateFin").value("2026-03-25"))
                .andExpect(jsonPath("$.nom").value("Mars 2026"));
    }

    @Test
    void refuserUnePeriodeQuiLaisseUnTrou() throws Exception {
        periode("2026-01-23", "2026-02-25");

        mvc.perform(en(moi, post("/periodes")).content("""
                        {"dateDebut": "2026-03-10"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void deplacerUneLimiteDeplaceLaPeriodeVoisine() throws Exception {
        periode("2026-01-23", "2026-02-25");
        Long mars = periode("2026-02-26", "2026-03-25");
        periode("2026-03-26", "2026-04-25");

        // Paie le 27 en mars : la période de mars se termine le 27, avril commence le 28
        mvc.perform(en(moi, put("/periodes/" + mars)).content("""
                        {"dateDebut": "2026-02-24", "dateFin": "2026-03-27"}"""))
                .andExpect(status().isOk());

        mvc.perform(en(moi, get("/periodes")))
                .andExpect(jsonPath("$[0].dateFin").value("2026-02-23"))
                .andExpect(jsonPath("$[1].dateDebut").value("2026-02-24"))
                .andExpect(jsonPath("$[1].dateFin").value("2026-03-27"))
                .andExpect(jsonPath("$[2].dateDebut").value("2026-03-28"));
    }

    @Test
    void refuserDeViderUnePeriodeVoisine() throws Exception {
        Long fevrier = periode("2026-01-23", "2026-02-25");
        periode("2026-02-26", "2026-03-25");

        mvc.perform(en(moi, put("/periodes/" + fevrier)).content("""
                        {"dateDebut": "2026-01-23", "dateFin": "2026-03-25"}"""))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void creerLesPeriodesManquantesJusquAUneDate() {
        periode("2026-01-23", "2026-02-25");

        periodeService.couvrirJusqua(moi.getId(), LocalDate.parse("2026-04-10"));

        assertThat(periodeRepository.findByUtilisateurIdOrderByDateDebut(moi.getId()))
                .extracting(p -> p.getDateDebut() + "→" + p.getDateFin())
                .containsExactly("2026-01-23→2026-02-25", "2026-02-26→2026-03-25", "2026-03-26→2026-04-25");
    }

    private Long periode(String debut, String fin) {
        Periode periode = new Periode();
        periode.setUtilisateur(moi);
        periode.setNom("Test");
        periode.setDateDebut(LocalDate.parse(debut));
        periode.setDateFin(LocalDate.parse(fin));
        return periodeRepository.save(periode).getId();
    }
}
