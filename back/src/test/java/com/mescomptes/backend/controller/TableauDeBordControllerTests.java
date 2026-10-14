package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TableauDeBordControllerTests extends ApiTestBase {

    @Autowired
    private PeriodeRepository periodeRepository;
    @Autowired
    private RecurrenceRepository recurrenceRepository;

    private Long janvier;
    private Long fevrier;
    private Long mars;
    private Compte courant;
    private Compte livret;

    /*
     * Janvier : salaire 2000, courses 100 (courant) ; courses 50 (livret)
     * Février : salaire 2000, courses 150, restaurant 80 (courant) ; courses 20 (livret)
     * Loyer de 700 € le 31 (ramené au 28 en février), pas encore généré pour février
     */
    @BeforeEach
    void creerDonnees() {
        janvier = periode("Janvier 2026", "2026-01-01", "2026-01-31");
        fevrier = periode("Février 2026", "2026-02-01", "2026-02-28");
        mars = periode("Mars 2026", "2026-03-01", "2026-03-31");

        courant = compte(moi, "Courant");
        courant.setSoldeInitial(new BigDecimal("1000.00"));
        courant.setDateSoldeInitial(LocalDate.parse("2025-12-31"));
        livret = compte(moi, "Livret");
        livret.setSoldeInitial(new BigDecimal("500.00"));
        livret.setDateSoldeInitial(LocalDate.parse("2025-12-31"));

        Categorie salaire = categorieFournie("Salaire");
        Categorie courses = categorieFournie("Courses");
        Categorie sorties = categorieFournie("Sorties & restaurants");
        operation(courant, salaire, null, "2026-01-02", "2000.00");
        operation(courant, courses, null, "2026-01-10", "100.00");
        operation(livret, courses, null, "2026-01-11", "50.00");
        operation(courant, salaire, null, "2026-02-02", "2000.00");
        operation(courant, courses, null, "2026-02-10", "150.00");
        operation(courant, sorties, null, "2026-02-14", "80.00");
        operation(livret, courses, null, "2026-02-12", "20.00");

        recurrence(categorieFournie("Logement"), "700.00", 31, "2026-02-28", null);
        // Prêt terminé avant sa prochaine occurrence : rien à venir
        recurrence(categorieFournie("Prêts"), "150.00", 15, "2026-02-15", "2026-02-10");
    }

    @Test
    void totauxEcartsEtSoldesDeLaPeriodePourTousLesComptes() throws Exception {
        mvc.perform(en(moi, get("/tableau-de-bord").param("periodeId", fevrier.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periode.nom").value("Février 2026"))
                .andExpect(jsonPath("$.periodePrecedenteId").value(janvier))
                .andExpect(jsonPath("$.periodeSuivanteId").value(mars))
                .andExpect(jsonPath("$.revenus").value(2000.00))
                .andExpect(jsonPath("$.depenses").value(250.00))
                .andExpect(jsonPath("$.solde").value(1750.00))
                .andExpect(jsonPath("$.parCategorie[*].nom", contains("Salaire", "Courses", "Sorties & restaurants")))
                .andExpect(jsonPath("$.parCategorie[1].total").value(170.00))
                .andExpect(jsonPath("$.parCategorie[1].totalPrecedent").value(150.00))
                .andExpect(jsonPath("$.parCategorie[1].ecart").value(20.00))
                .andExpect(jsonPath("$.parCategorie[2].ecart").value(80.00))
                .andExpect(jsonPath("$.aVenir.length()").value(1))
                .andExpect(jsonPath("$.aVenir[0].date").value("2026-02-28"))
                .andExpect(jsonPath("$.aVenir[0].montant").value(700.00))
                .andExpect(jsonPath("$.soldePrevu").value(1050.00))
                // Courant : 1000 + 1900 + 1770 ; livret : 500 - 70 ; moins le loyer à venir
                .andExpect(jsonPath("$.soldeComptesFinPeriode").value(4400.00))
                .andExpect(jsonPath("$.dernieresOperations[*].date", contains("2026-02-14", "2026-02-12", "2026-02-10", "2026-02-02")));
    }

    @Test
    void limiterLeTableauDeBordAUnCompte() throws Exception {
        mvc.perform(en(moi, get("/tableau-de-bord").param("periodeId", fevrier.toString()).param("compteId", livret.getId().toString())))
                .andExpect(jsonPath("$.compteId").value(livret.getId()))
                .andExpect(jsonPath("$.revenus").value(0))
                .andExpect(jsonPath("$.depenses").value(20.00))
                .andExpect(jsonPath("$.parCategorie.length()").value(1))
                .andExpect(jsonPath("$.parCategorie[0].ecart").value(-30.00))
                .andExpect(jsonPath("$.aVenir.length()").value(0))
                .andExpect(jsonPath("$.soldeComptesFinPeriode").value(430.00));
    }

    @Test
    void premierePeriodeSansPrecedenteNiEcartAuDela() throws Exception {
        mvc.perform(en(moi, get("/tableau-de-bord").param("periodeId", janvier.toString())))
                .andExpect(jsonPath("$.periodePrecedenteId").doesNotExist())
                .andExpect(jsonPath("$.parCategorie[0].nom").value("Salaire"))
                .andExpect(jsonPath("$.parCategorie[0].ecart").value(2000.00))
                .andExpect(jsonPath("$.aVenir.length()").value(0));
    }

    @Test
    void sansPeriodeDemandeeAfficherLaDerniereSiAucuneNeContientAujourdHui() throws Exception {
        // Les périodes de test sont toutes passées
        mvc.perform(en(moi, get("/tableau-de-bord"))).andExpect(jsonPath("$.periode.id").value(mars));
    }

    @Test
    void sansPeriodeDemandeeAfficherCelleQuiContientAujourdHui() throws Exception {
        LocalDate aujourdHui = LocalDate.now();
        Long enCours = periode("En cours", aujourdHui.minusDays(3).toString(), aujourdHui.plusDays(3).toString());
        periode("Après", aujourdHui.plusDays(4).toString(), aujourdHui.plusDays(30).toString());

        mvc.perform(en(moi, get("/tableau-de-bord"))).andExpect(jsonPath("$.periode.id").value(enCours));
    }

    @Test
    void refuserSansPeriodeOuAvecLesDonneesDUnAutre() throws Exception {
        mvc.perform(en(autre, get("/tableau-de-bord"))).andExpect(status().isUnprocessableContent());
        mvc.perform(en(autre, get("/tableau-de-bord").param("periodeId", fevrier.toString()))).andExpect(status().isNotFound());
        mvc.perform(en(moi, get("/tableau-de-bord").param("compteId", compte(autre, "Autre").getId().toString())))
                .andExpect(status().isNotFound());
    }

    private Long periode(String nom, String debut, String fin) {
        Periode periode = new Periode();
        periode.setUtilisateur(moi);
        periode.setNom(nom);
        periode.setDateDebut(LocalDate.parse(debut));
        periode.setDateFin(LocalDate.parse(fin));
        return periodeRepository.save(periode).getId();
    }

    private void recurrence(Categorie categorie, String montant, int jour, String prochaine, String fin) {
        Recurrence recurrence = new Recurrence();
        recurrence.setUtilisateur(moi);
        recurrence.setCompte(courant);
        recurrence.setCategorie(categorie);
        recurrence.setType(categorie.getType());
        recurrence.setMontant(new BigDecimal(montant));
        recurrence.setFrequence(Frequence.MENSUELLE);
        recurrence.setJour(jour);
        recurrence.setDateDebut(LocalDate.parse("2026-01-01"));
        recurrence.setProchaineOccurrence(LocalDate.parse(prochaine));
        recurrence.setDateFin(fin == null ? null : LocalDate.parse(fin));
        recurrenceRepository.save(recurrence);
    }
}
