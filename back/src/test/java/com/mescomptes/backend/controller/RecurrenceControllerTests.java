package com.mescomptes.backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.repository.PeriodeRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import com.mescomptes.backend.service.GenerateurRecurrences;
import com.mescomptes.backend.service.PlanificationRecurrences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecurrenceControllerTests extends ApiTestBase {

    private static final LocalDate AUJOURD_HUI = LocalDate.now();
    // Le 1er d'il y a deux mois : trois occurrences du 1er sont déjà échues (M-2, M-1 et ce mois-ci)
    private static final LocalDate IL_Y_A_DEUX_MOIS = AUJOURD_HUI.withDayOfMonth(1).minusMonths(2);

    @Autowired
    private RecurrenceRepository recurrenceRepository;
    @Autowired
    private PeriodeRepository periodeRepository;
    @Autowired
    private GenerateurRecurrences generateur;
    @Autowired
    private PlanificationRecurrences planification;

    private Compte courant;
    private Categorie logement;

    @BeforeEach
    void creerCompte() {
        courant = compte(moi, "Courant");
        logement = categorieFournie("Logement");
    }

    // ----- Création et génération -----

    @Test
    void creerUneRecurrenceGenereLesOccurrencesDejaEchues() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        assertThat(occurrences(id)).extracting(Operation::getDate)
                .containsExactlyInAnyOrder(IL_Y_A_DEUX_MOIS, IL_Y_A_DEUX_MOIS.plusMonths(1), IL_Y_A_DEUX_MOIS.plusMonths(2));
        assertThat(occurrences(id)).allSatisfy(o -> {
            assertThat(o.getMontant()).isEqualByComparingTo("700.00");
            assertThat(o.getLibelle().getNom()).isEqualTo("Loyer");
        });
        assertThat(recurrenceRepository.findById(id).orElseThrow().getProchaineOccurrence()).isEqualTo(IL_Y_A_DEUX_MOIS.plusMonths(3));
        // Les périodes nécessaires ont été créées
        assertThat(periodeRepository.findContenant(moi.getId(), IL_Y_A_DEUX_MOIS)).isPresent();
    }

    @Test
    void deduireLeJourEtLaCategorie() throws Exception {
        mvc.perform(en(moi, post("/recurrences")).content("""
                        {"compteId": %d, "montant": 2100, "libelleId": %d, "frequence": "MENSUELLE", "dateDebut": "%s"}"""
                        .formatted(courant.getId(), libelleFourni("Salaire").getId(), AUJOURD_HUI.plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jour").value(AUJOURD_HUI.plusDays(1).getDayOfMonth()))
                .andExpect(jsonPath("$.type").value("REVENU"))
                .andExpect(jsonPath("$.categorieId").value(categorieFournie("Salaire").getId()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void neJamaisGenererDeuxFoisNiRecreerUneOccurrenceSupprimee() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        assertThat(generateur.generer(id, AUJOURD_HUI)).isZero();

        Operation premiere = occurrences(id).stream().filter(o -> o.getDate().equals(IL_Y_A_DEUX_MOIS)).findFirst().orElseThrow();
        mvc.perform(en(moi, delete("/operations/" + premiere.getId()))).andExpect(status().isNoContent());
        planification.rattraper(moi.getId());

        assertThat(occurrences(id)).hasSize(2);
    }

    @Test
    void rattraperLesOccurrencesManqueesPendantQueLeServeurEtaitArrete() {
        Recurrence enRetard = new Recurrence();
        enRetard.setUtilisateur(moi);
        enRetard.setCompte(courant);
        enRetard.setCategorie(logement);
        enRetard.setType(logement.getType());
        enRetard.setMontant(new BigDecimal("30.00"));
        enRetard.setFrequence(Frequence.MENSUELLE);
        enRetard.setJour(1);
        enRetard.setDateDebut(IL_Y_A_DEUX_MOIS);
        enRetard.setProchaineOccurrence(IL_Y_A_DEUX_MOIS.plusMonths(1));
        recurrenceRepository.save(enRetard);

        planification.rattraper(moi.getId());

        assertThat(occurrences(enRetard.getId())).hasSize(2);
        assertThat(enRetard.getProchaineOccurrence()).isEqualTo(IL_Y_A_DEUX_MOIS.plusMonths(3));
    }

    // ----- Modification -----

    @Test
    void modifierLeMontantNeChangeQueLesOccurrencesFutures() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        mvc.perform(en(moi, put("/recurrences/" + id)).content(loyer(IL_Y_A_DEUX_MOIS, "750", 1, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montant").value(750))
                .andExpect(jsonPath("$.prochaineOccurrence").value(IL_Y_A_DEUX_MOIS.plusMonths(3).toString()));

        assertThat(occurrences(id)).hasSize(3).allSatisfy(o -> assertThat(o.getMontant()).isEqualByComparingTo("700.00"));
    }

    @Test
    void changerLeJourRepartDeDemainSansRattraperLePasse() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        mvc.perform(en(moi, put("/recurrences/" + id)).content(loyer(IL_Y_A_DEUX_MOIS, "700", 28, null)))
                .andExpect(status().isOk());

        LocalDate prochaine = recurrenceRepository.findById(id).orElseThrow().getProchaineOccurrence();
        assertThat(prochaine).isAfter(AUJOURD_HUI);
        assertThat(prochaine.getDayOfMonth()).isEqualTo(28);
        assertThat(occurrences(id)).hasSize(3);
    }

    @Test
    void refuserUneSaisieIncoherente() throws Exception {
        mvc.perform(en(moi, post("/recurrences")).content(loyer(IL_Y_A_DEUX_MOIS, "700", 32, null)))
                .andExpect(status().isBadRequest());
        mvc.perform(en(moi, post("/recurrences")).content(loyer(IL_Y_A_DEUX_MOIS, "700", 1, IL_Y_A_DEUX_MOIS.minusDays(1).toString())))
                .andExpect(status().isUnprocessableContent());
    }

    // ----- Arrêt -----

    @Test
    void arreterUneRecurrenceGardeLHistoriqueEtNeGenerePlusRien() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        mvc.perform(en(moi, delete("/recurrences/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateFin").value(AUJOURD_HUI.toString()))
                .andExpect(jsonPath("$.active").value(false));

        assertThat(generateur.generer(id, AUJOURD_HUI.plusMonths(6))).isZero();
        assertThat(occurrences(id)).hasSize(3);
    }

    @Test
    void supprimerUneRecurrenceQuiNAEncoreRienGenere() throws Exception {
        Long id = creerLoyer(AUJOURD_HUI.plusMonths(1));

        mvc.perform(en(moi, delete("/recurrences/" + id))).andExpect(status().isNoContent());
        assertThat(recurrenceRepository.findById(id)).isEmpty();
    }

    @Test
    void reprendreUneRecurrenceArreteeSansRattraperLaPause() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);
        Recurrence recurrence = recurrenceRepository.findById(id).orElseThrow();
        // Arrêtée depuis longtemps, prochaine occurrence restée dans le passé
        recurrence.setDateFin(IL_Y_A_DEUX_MOIS);
        recurrence.setProchaineOccurrence(IL_Y_A_DEUX_MOIS.plusMonths(1));

        mvc.perform(en(moi, put("/recurrences/" + id)).content(loyer(IL_Y_A_DEUX_MOIS, "700", 1, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(recurrence.getProchaineOccurrence()).isAfter(AUJOURD_HUI);
        assertThat(occurrences(id)).hasSize(3);
    }

    @Test
    void neJamaisToucherLaRecurrenceDUnAutreUtilisateur() throws Exception {
        Long id = creerLoyer(IL_Y_A_DEUX_MOIS);

        mvc.perform(en(autre, put("/recurrences/" + id)).content(loyer(IL_Y_A_DEUX_MOIS, "1", 1, null)))
                .andExpect(status().isNotFound());
        mvc.perform(en(autre, delete("/recurrences/" + id))).andExpect(status().isNotFound());
        mvc.perform(en(autre, get("/recurrences"))).andExpect(jsonPath("$.length()").value(0));
    }

    // ----- Répéter une opération -----

    @Test
    void repeterChaqueMoisUneOperationSaisieALaMain() throws Exception {
        Operation netflix = operation(courant, categorieFournie("Abonnements & loisirs"), libelleFourni("Netflix"),
                AUJOURD_HUI.toString(), "13.49");

        String reponse = mvc.perform(en(moi, post("/operations/" + netflix.getId() + "/repeter")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frequence").value("MENSUELLE"))
                .andExpect(jsonPath("$.jour").value(AUJOURD_HUI.getDayOfMonth()))
                .andExpect(jsonPath("$.montant").value(13.49))
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(reponse, "$.id")).longValue();

        assertThat(netflix.getRecurrence().getId()).isEqualTo(id);
        // L'opération est la première occurrence : rien d'autre n'est généré
        assertThat(occurrences(id)).containsExactly(netflix);
        mvc.perform(en(moi, post("/operations/" + netflix.getId() + "/repeter"))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void repeterChaqueAnnee() throws Exception {
        Operation assurance = operation(courant, categorieFournie("Assurances"), null, AUJOURD_HUI.toString(), "420.00");

        mvc.perform(en(moi, post("/operations/" + assurance.getId() + "/repeter").param("frequence", "ANNUELLE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prochaineOccurrence").value(AUJOURD_HUI.plusYears(1).toString()));
    }

    // ----- Utilitaires -----

    private Long creerLoyer(LocalDate debut) throws Exception {
        String reponse = mvc.perform(en(moi, post("/recurrences")).content(loyer(debut, "700", 1, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(reponse, "$.id")).longValue();
    }

    private String loyer(LocalDate debut, String montant, int jour, String fin) {
        return """
                {"compteId": %d, "montant": %s, "libelleId": %d, "frequence": "MENSUELLE", "jour": %d, "dateDebut": "%s", "dateFin": %s}"""
                .formatted(courant.getId(), montant, libelleFourni("Loyer").getId(), jour, debut, fin == null ? "null" : "\"" + fin + "\"");
    }

    private List<Operation> occurrences(Long recurrenceId) {
        return operationRepository.findAll().stream()
                .filter(o -> o.getRecurrence() != null && o.getRecurrence().getId().equals(recurrenceId))
                .toList();
    }
}
