package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Libelle;
import com.mescomptes.backend.model.entities.Utilisateur;
import com.mescomptes.backend.model.enums.TypeOperation;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategorieEtLibelleControllerTests extends ApiTestBase {

    // ----- Catégories -----

    @Test
    void listerLesCategoriesFourniesEtPersonnelles() throws Exception {
        categoriePerso(moi, "Animaux");
        categoriePerso(autre, "Chez autre");

        mvc.perform(en(moi, get("/categories")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(22))
                .andExpect(jsonPath("$[?(@.nom == 'Animaux')].fournie").value(false))
                .andExpect(jsonPath("$[?(@.nom == 'Courses')].fournie").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'Chez autre')]").isEmpty());
    }

    @Test
    void creerUneCategoriePersonnelle() throws Exception {
        mvc.perform(en(moi, post("/categories")).content("""
                        {"nom": " Animaux ", "type": "DEPENSE", "icone": "🐶", "couleur": "#A1887F"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Animaux"))
                .andExpect(jsonPath("$.fournie").value(false));
    }

    @Test
    void refuserUnNomDejaPrisSansTenirCompteDeLaCasseNiDesEspaces() throws Exception {
        categoriePerso(moi, "Animaux");

        creerCategorie(moi, "  COURSES ").andExpect(status().isConflict());
        creerCategorie(moi, "animaux").andExpect(status().isConflict());
        // Un autre utilisateur peut avoir sa propre catégorie du même nom
        creerCategorie(autre, "Animaux").andExpect(status().isCreated());
    }

    @Test
    void refuserUneCouleurMalFormee() throws Exception {
        mvc.perform(en(moi, post("/categories")).content("""
                        {"nom": "Animaux", "type": "DEPENSE", "couleur": "rouge"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void neJamaisModifierNiSupprimerUneCategorieFournie() throws Exception {
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, put("/categories/" + courses)).content("""
                        {"nom": "Mes courses", "type": "DEPENSE"}"""))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, delete("/categories/" + courses))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void neJamaisToucherLaCategorieDUnAutreUtilisateur() throws Exception {
        Categorie chezLAutre = categoriePerso(autre, "Secret");

        mvc.perform(en(moi, put("/categories/" + chezLAutre.getId())).content("""
                        {"nom": "Piraté", "type": "DEPENSE"}"""))
                .andExpect(status().isNotFound());
        mvc.perform(en(moi, delete("/categories/" + chezLAutre.getId()))).andExpect(status().isNotFound());
    }

    @Test
    void supprimerUneCategorieInutiliseeEtArchiverUneCategorieUtilisee() throws Exception {
        Categorie inutilisee = categoriePerso(moi, "Inutilisée");
        Categorie utilisee = categoriePerso(moi, "Utilisée");
        libellePerso(moi, "Croquettes", utilisee);

        mvc.perform(en(moi, delete("/categories/" + inutilisee.getId()))).andExpect(status().isNoContent());
        mvc.perform(en(moi, delete("/categories/" + utilisee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive").value(true));
    }

    @Test
    void refuserDeChangerLeTypeDUneCategorieUtilisee() throws Exception {
        Categorie animaux = categoriePerso(moi, "Animaux");
        operation(compte(moi, "Courant"), animaux, null, "2026-03-01", "20.00");

        mvc.perform(en(moi, put("/categories/" + animaux.getId())).content("""
                        {"nom": "Animaux", "type": "REVENU"}"""))
                .andExpect(status().isUnprocessableContent());
        // Le reste reste modifiable
        mvc.perform(en(moi, put("/categories/" + animaux.getId())).content("""
                        {"nom": "Animaux de compagnie", "type": "DEPENSE"}"""))
                .andExpect(status().isOk());
    }

    // ----- Libellés -----

    @Test
    void creerUnLibelleDepuisLaSaisie() throws Exception {
        Long courses = categorieFournie("Courses").getId();

        mvc.perform(en(moi, post("/libelles")).content("""
                        {"nom": "Primeur du coin", "categorieId": %d}""".formatted(courses)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorieId").value(courses))
                .andExpect(jsonPath("$.fourni").value(false));
        mvc.perform(en(moi, post("/libelles")).content("""
                        {"nom": "netflix", "categorieId": %d}""".formatted(courses)))
                .andExpect(status().isConflict());
    }

    @Test
    void refuserUnLibelleSurLaCategorieDUnAutreUtilisateur() throws Exception {
        Categorie chezLAutre = categoriePerso(autre, "Secret");

        mvc.perform(en(moi, post("/libelles")).content("""
                        {"nom": "Test", "categorieId": %d}""".formatted(chezLAutre.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void autocompleterEnProposantLesPlusUtilisesDAbord() throws Exception {
        mvc.perform(en(moi, get("/libelles").param("q", "pr")))
                .andExpect(jsonPath("$[*].nom", hasItems("Prime", "Prêt auto", "Prévoyance")));

        Compte compte = compte(moi, "Courant");
        Libelle prime = libelleFourni("Prime");
        operation(compte, categorieFournie("Salaire"), prime, "2026-03-01", "300.00");
        operation(compte, categorieFournie("Salaire"), prime, "2026-04-01", "300.00");

        mvc.perform(en(moi, get("/libelles").param("q", " PR ")))
                .andExpect(jsonPath("$[0].nom").value("Prime"));
        // L'usage des autres utilisateurs ne compte pas
        mvc.perform(en(autre, get("/libelles").param("q", "pr")))
                .andExpect(jsonPath("$[0].nom").value(not("Prime")));
    }

    @Test
    void autocompleterSansInterpreterLesCaracteresSpeciaux() throws Exception {
        mvc.perform(en(moi, get("/libelles").param("q", "%"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(en(moi, get("/libelles").param("q", "_"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void neJamaisProposerLesLibellesDUnAutreUtilisateur() throws Exception {
        libellePerso(autre, "Zzz secret", categorieFournie("Courses"));

        mvc.perform(en(moi, get("/libelles").param("q", "zzz"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(en(autre, get("/libelles").param("q", "zzz"))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void supprimerUnLibelleInutiliseEtArchiverUnLibelleUtilise() throws Exception {
        Categorie courses = categorieFournie("Courses");
        Libelle inutilise = libellePerso(moi, "Inutilisé", courses);
        Libelle utilise = libellePerso(moi, "Utilisé", courses);
        operation(compte(moi, "Courant"), courses, utilise, "2026-03-01", "5.00");

        mvc.perform(en(moi, delete("/libelles/" + inutilise.getId()))).andExpect(status().isNoContent());
        mvc.perform(en(moi, delete("/libelles/" + utilise.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive").value(true));
        // Archivé : plus proposé à la saisie
        mvc.perform(en(moi, get("/libelles").param("q", "utilisé"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unPutSansArchiveGardeLEtatDArchivage() throws Exception {
        Categorie animaux = categoriePerso(moi, "Animaux");
        animaux.setArchive(true);
        categorieRepository.save(animaux);
        Libelle croquettes = libellePerso(moi, "Croquettes", categorieFournie("Courses"));
        croquettes.setArchive(true);
        libelleRepository.save(croquettes);

        mvc.perform(en(moi, put("/categories/" + animaux.getId())).content("""
                        {"nom": "Animaux de compagnie", "type": "DEPENSE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive").value(true));
        mvc.perform(en(moi, put("/libelles/" + croquettes.getId())).content("""
                        {"nom": "Croquettes chat", "categorieId": %d}""".formatted(categorieFournie("Courses").getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archive").value(true));
        mvc.perform(en(moi, put("/libelles/" + croquettes.getId())).content("""
                        {"nom": "Croquettes chat", "categorieId": %d, "archive": false}""".formatted(categorieFournie("Courses").getId())))
                .andExpect(jsonPath("$.archive").value(false));
    }

    // ----- Masquage -----

    @Test
    void masquerPuisReafficherUnLibelleFourni() throws Exception {
        Long netflix = libelleFourni("Netflix").getId();

        mvc.perform(en(moi, put("/masquages/libelle/" + netflix))).andExpect(status().isNoContent());
        // Idempotent
        mvc.perform(en(moi, put("/masquages/libelle/" + netflix))).andExpect(status().isNoContent());

        mvc.perform(en(moi, get("/libelles").param("q", "netflix"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(en(moi, get("/libelles"))).andExpect(jsonPath("$[?(@.nom == 'Netflix')].masque").value(true));
        // Le masquage ne concerne que celui qui l'a fait
        mvc.perform(en(autre, get("/libelles").param("q", "netflix"))).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(en(moi, delete("/masquages/libelle/" + netflix))).andExpect(status().isNoContent());
        mvc.perform(en(moi, get("/libelles").param("q", "netflix"))).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void masquerUneCategorieMasqueSesLibellesALaSaisie() throws Exception {
        Long sport = categorieFournie("Sport").getId();

        mvc.perform(en(moi, put("/masquages/categorie/" + sport))).andExpect(status().isNoContent());

        mvc.perform(en(moi, get("/categories"))).andExpect(jsonPath("$[?(@.nom == 'Sport')].masquee").value(true));
        mvc.perform(en(moi, get("/libelles").param("q", "salle de sport"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void refuserDeMasquerUnElementPersonnelOuUnTypeInconnu() throws Exception {
        Categorie animaux = categoriePerso(moi, "Animaux");

        mvc.perform(en(moi, put("/masquages/categorie/" + animaux.getId()))).andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, put("/masquages/compte/1"))).andExpect(status().isNotFound());
        assertThat(categorieRepository.findById(animaux.getId())).isPresent();
    }

    @Test
    void reafficherExigeUnElementFourniQuiExiste() throws Exception {
        Categorie animaux = categoriePerso(moi, "Animaux");
        Categorie chezLAutre = categoriePerso(autre, "Bricolage perso");

        mvc.perform(en(moi, delete("/masquages/libelle/999999999"))).andExpect(status().isNotFound());
        mvc.perform(en(moi, delete("/masquages/categorie/" + chezLAutre.getId()))).andExpect(status().isNotFound());
        mvc.perform(en(moi, delete("/masquages/categorie/" + animaux.getId()))).andExpect(status().isUnprocessableContent());
        mvc.perform(en(moi, delete("/masquages/compte/1"))).andExpect(status().isNotFound());
    }

    // ----- Utilitaires -----

    private ResultActions creerCategorie(Utilisateur utilisateur, String nom) throws Exception {
        return mvc.perform(en(utilisateur, post("/categories")).content("""
                {"nom": "%s", "type": "DEPENSE"}""".formatted(nom)));
    }

    private Categorie categoriePerso(Utilisateur utilisateur, String nom) {
        Categorie categorie = new Categorie();
        categorie.setUtilisateur(utilisateur);
        categorie.setNom(nom);
        categorie.setType(TypeOperation.DEPENSE);
        return categorieRepository.save(categorie);
    }

    private Libelle libellePerso(Utilisateur utilisateur, String nom, Categorie categorie) {
        Libelle libelle = new Libelle();
        libelle.setUtilisateur(utilisateur);
        libelle.setNom(nom);
        libelle.setCategorieParDefaut(categorie);
        return libelleRepository.save(libelle);
    }
}
