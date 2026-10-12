package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Periode;
import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.repository.PeriodeRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OperationControllerTests extends ApiTestBase {

    @Autowired
    private PeriodeRepository periodeRepository;
    @Autowired
    private RecurrenceRepository recurrenceRepository;

    private Compte courant;

    @BeforeEach
    void creerCompte() {
        courant = compte(moi, "Courant");
    }

    // ----- Création -----

    @Test
    void deduireLaCategorieEtLeTypeDuLibelleEtArrondirAuCentime() throws Exception {
        Long netflix = libelleFourni("Netflix").getId();

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 93.1375, "libelleId": %d, "note": "  abonnement  "}"""
                        .formatted(courant.getId(), netflix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorieId").value(categorieFournie("Abonnements & loisirs").getId()))
                .andExpect(jsonPath("$.type").value("DEPENSE"))
                .andExpect(jsonPath("$.montant").value(93.14))
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.note").value("abonnement"));
    }

    @Test
    void garderLaCategorieSaisieMemeSiLeLibelleEnProposeUneAutre() throws Exception {
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 12, "libelleId": %d, "categorieId": %d}"""
                        .formatted(courant.getId(), libelleFourni("Restaurant").getId(), courses)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorieId").value(courses));
    }

    @Test
    void deduireLeTypeRevenuDeLaCategorie() throws Exception {
        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 2100, "categorieId": %d}"""
                        .formatted(courant.getId(), categorieFournie("Salaire").getId())))
                .andExpect(jsonPath("$.type").value("REVENU"));
    }

    @Test
    void refuserUneOperationSansCategorieNiLibelle() throws Exception {
        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 10}""".formatted(courant.getId())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void refuserUnMontantNegatifOuNulApresArrondi() throws Exception {
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": -5, "categorieId": %d}""".formatted(courant.getId(), courses)))
                .andExpect(status().isBadRequest());
        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 0.004, "categorieId": %d}""".formatted(courant.getId(), courses)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void creerLesPeriodesManquantes() throws Exception {
        periode("2026-01-23", "2026-02-25");

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "date": "2026-04-02", "montant": 10, "categorieId": %d}"""
                        .formatted(courant.getId(), categorieFournie("Courses").getId())))
                .andExpect(status().isCreated());

        assertThat(periodeRepository.findByUtilisateurIdOrderByDateDebut(moi.getId())).hasSize(3);
    }

    @Test
    void refuserLesElementsDUnAutreUtilisateur() throws Exception {
        Compte compteAutre = compte(autre, "Secret");
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 10, "categorieId": %d}""".formatted(compteAutre.getId(), courses)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refuserUnCompteArchiveSaufSurUneOperationQuiLUtiliseDeja() throws Exception {
        Operation ancienne = operation(courant, categorieFournie("Courses"), null, "2026-01-10", "10.00");
        courant.setArchive(true);
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, post("/operations")).content("""
                        {"compteId": %d, "montant": 10, "categorieId": %d}""".formatted(courant.getId(), courses)))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, put("/operations/" + ancienne.getId())).content("""
                        {"compteId": %d, "date": "2026-01-10", "montant": 11, "categorieId": %d}""".formatted(courant.getId(), courses)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(11));
    }

    // ----- Liste et filtres -----

    @Test
    void filtrerParPeriodeCompteCategorieEtTexte() throws Exception {
        Long fevrier = periode("2026-01-23", "2026-02-25");
        Compte livret = compte(moi, "Livret");
        var courses = categorieFournie("Courses");
        var sorties = categorieFournie("Sorties & restaurants");
        operation(courant, courses, libelleFourni("Supermarché"), "2026-02-01", "40.00");
        Operation resto = operation(courant, sorties, libelleFourni("Restaurant"), "2026-02-10", "30.00");
        resto.setNote("Resto anniv Camille");
        operation(livret, courses, null, "2026-02-15", "5.00");
        operation(courant, courses, null, "2026-03-01", "99.00");                 // hors période
        operation(compte(autre, "Autre"), courses, null, "2026-02-01", "1.00");    // autre utilisateur

        mvc.perform(en(moi, get("/operations").param("periodeId", fevrier.toString())))
                .andExpect(jsonPath("$[*].date", contains("2026-02-15", "2026-02-10", "2026-02-01")));
        mvc.perform(en(moi, get("/operations").param("periodeId", fevrier.toString()).param("compteId", livret.getId().toString())))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(en(moi, get("/operations").param("categorieId", courses.getId().toString())))
                .andExpect(jsonPath("$.length()").value(3));
        // Texte cherché dans la note…
        mvc.perform(en(moi, get("/operations").param("q", "camille")))
                .andExpect(jsonPath("$[*].id", contains(resto.getId().intValue())));
        // … et dans le libellé
        mvc.perform(en(moi, get("/operations").param("q", "SUPER")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refuserLaPeriodeDUnAutreUtilisateur() throws Exception {
        Periode chezLAutre = new Periode();
        chezLAutre.setUtilisateur(autre);
        chezLAutre.setNom("Test");
        chezLAutre.setDateDebut(LocalDate.parse("2026-01-01"));
        chezLAutre.setDateFin(LocalDate.parse("2026-01-31"));
        periodeRepository.save(chezLAutre);

        mvc.perform(en(moi, get("/operations").param("periodeId", chezLAutre.getId().toString())))
                .andExpect(status().isNotFound());
    }

    // ----- Modification et suppression -----

    @Test
    void modifierUneOccurrenceGardeLeLienAvecSaRecurrence() throws Exception {
        var telephone = categorieFournie("Téléphone & internet");
        Recurrence forfait = recurrence(telephone);
        Operation occurrence = operation(courant, telephone, null, "2026-03-05", "20.17");
        occurrence.setRecurrence(forfait);

        mvc.perform(en(moi, put("/operations/" + occurrence.getId())).content("""
                        {"compteId": %d, "date": "2026-03-05", "montant": 17.99, "categorieId": %d}"""
                        .formatted(courant.getId(), telephone.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(17.99))
                .andExpect(jsonPath("$.recurrenceId").value(forfait.getId()));
        assertThat(forfait.getMontant()).isEqualByComparingTo("20.17");
    }

    @Test
    void supprimerUneOperation() throws Exception {
        Operation operation = operation(courant, categorieFournie("Courses"), null, "2026-03-01", "10.00");
        Operation chezLAutre = operation(compte(autre, "Autre"), categorieFournie("Courses"), null, "2026-03-01", "10.00");

        mvc.perform(en(moi, delete("/operations/" + chezLAutre.getId()))).andExpect(status().isNotFound());
        mvc.perform(en(moi, delete("/operations/" + operation.getId()))).andExpect(status().isNoContent());
        assertThat(operationRepository.findById(operation.getId())).isEmpty();
    }

    @Test
    void proposerLeDernierCompteUtilise() throws Exception {
        Compte livret = compte(moi, "Livret");
        operation(courant, categorieFournie("Courses"), null, "2026-03-01", "10.00");
        operation(livret, categorieFournie("Courses"), null, "2026-02-01", "10.00");

        mvc.perform(en(moi, get("/comptes")))
                .andExpect(jsonPath("$[?(@.nom == 'Livret')].dernierUtilise").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'Courant')].dernierUtilise").value(false));
    }

    // ----- Utilitaires -----

    private Long periode(String debut, String fin) {
        Periode periode = new Periode();
        periode.setUtilisateur(moi);
        periode.setNom("Test");
        periode.setDateDebut(LocalDate.parse(debut));
        periode.setDateFin(LocalDate.parse(fin));
        return periodeRepository.save(periode).getId();
    }

    private Recurrence recurrence(Categorie categorie) {
        Recurrence recurrence = new Recurrence();
        recurrence.setUtilisateur(moi);
        recurrence.setCompte(courant);
        recurrence.setCategorie(categorie);
        recurrence.setType(categorie.getType());
        recurrence.setMontant(new BigDecimal("20.17"));
        recurrence.setFrequence(Frequence.MENSUELLE);
        recurrence.setJour(5);
        recurrence.setDateDebut(LocalDate.parse("2026-01-05"));
        recurrence.setProchaineOccurrence(LocalDate.parse("2026-04-05"));
        return recurrenceRepository.save(recurrence);
    }
}
