package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Libelle;
import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Utilisateur;
import com.mescomptes.backend.repository.CategorieRepository;
import com.mescomptes.backend.repository.CompteRepository;
import com.mescomptes.backend.repository.LibelleRepository;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Base des tests d'API : deux utilisateurs neufs par test et des raccourcis pour créer des données.
 * Chaque test est annulé à la fin (@Transactional) : la base de dev n'est pas modifiée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UtilisateurRepository utilisateurRepository;
    @Autowired
    protected CompteRepository compteRepository;
    @Autowired
    protected CategorieRepository categorieRepository;
    @Autowired
    protected LibelleRepository libelleRepository;
    @Autowired
    protected OperationRepository operationRepository;

    protected Utilisateur moi;
    protected Utilisateur autre;

    @BeforeEach
    void creerUtilisateurs() {
        moi = creerUtilisateur();
        autre = creerUtilisateur();
    }

    // Requête JSON authentifiée en tant qu'utilisateur
    protected MockHttpServletRequestBuilder en(Utilisateur utilisateur, MockHttpServletRequestBuilder requete) {
        return requete.with(user(new CustomUserDetails(utilisateur))).contentType(MediaType.APPLICATION_JSON);
    }

    protected Utilisateur creerUtilisateur() {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        Utilisateur u = new Utilisateur();
        u.setPseudo("test-" + suffixe);
        u.setIdentifiant("test-" + suffixe);
        u.setEmail("test-" + suffixe + "@mescomptes.local");
        u.setMotDePasse("inutilisé");
        return utilisateurRepository.save(u);
    }

    protected Compte compte(Utilisateur utilisateur, String nom) {
        Compte compte = new Compte();
        compte.setUtilisateur(utilisateur);
        compte.setNom(nom);
        compte.setDateSoldeInitial(LocalDate.parse("2026-01-01"));
        return compteRepository.save(compte);
    }

    protected Categorie categorieFournie(String nom) {
        return categorieRepository.findFournies().stream().filter(c -> c.getNom().equals(nom)).findFirst().orElseThrow();
    }

    protected Libelle libelleFourni(String nom) {
        return libelleRepository.findFournis().stream().filter(l -> l.getNom().equals(nom)).findFirst().orElseThrow();
    }

    protected Operation operation(Compte compte, Categorie categorie, Libelle libelle, String date, String montant) {
        Operation operation = new Operation();
        operation.setCompte(compte);
        operation.setCategorie(categorie);
        operation.setLibelle(libelle);
        operation.setType(categorie.getType());
        operation.setDate(LocalDate.parse(date));
        operation.setMontant(new BigDecimal(montant));
        return operationRepository.save(operation);
    }
}
