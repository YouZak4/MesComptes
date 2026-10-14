package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Libelle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Règles de saisie communes aux opérations et aux récurrences :
 * <ul>
 *     <li>compte, catégorie et libellé fournis ou appartenant à l'utilisateur (sinon 404) ;</li>
 *     <li>catégorie saisie, sinon celle proposée par le libellé ;</li>
 *     <li>élément archivé refusé, sauf s'il était déjà là (on peut corriger une vieille opération) ;</li>
 *     <li>montant arrondi au centime, au moins 0,01 €.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SaisieOperationResolver {

    private final CompteService compteService;
    private final CategorieService categorieService;
    private final LibelleService libelleService;

    public record Saisie(Long compteId, Long categorieId, Long libelleId, BigDecimal montant) {
    }

    public record Resolue(Compte compte, Categorie categorie, Libelle libelle, BigDecimal montant) {
    }

    /**
     * @param saisie   identifiants et montant saisis
     * @param existant valeurs actuelles de l'élément modifié (null à la création)
     */
    public Resolue resoudre(UUID utilisateurId, Saisie saisie, Resolue existant) {
        Compte compte = compteService.trouver(utilisateurId, saisie.compteId());
        if (compte.isArchive() && change(existant == null ? null : existant.compte(), compte.getId(), Compte::getId)) {
            throw new BusinessRuleException("Le compte « " + compte.getNom() + " » est archivé.");
        }

        Libelle libelle = saisie.libelleId() == null ? null : libelleService.trouverVisible(utilisateurId, saisie.libelleId());
        if (libelle != null && libelle.isArchive() && change(existant == null ? null : existant.libelle(), libelle.getId(), Libelle::getId)) {
            throw new BusinessRuleException("Le libellé « " + libelle.getNom() + " » est archivé.");
        }

        Long categorieId = saisie.categorieId() != null ? saisie.categorieId()
                : libelle != null ? libelle.getCategorieParDefaut().getId() : null;
        if (categorieId == null) throw new BusinessRuleException("La catégorie est obligatoire.");
        Categorie categorie = categorieService.trouverVisible(utilisateurId, categorieId);
        if (categorie.isArchive() && change(existant == null ? null : existant.categorie(), categorie.getId(), Categorie::getId)) {
            throw new BusinessRuleException("La catégorie « " + categorie.getNom() + " » est archivée.");
        }

        // Arrondi au centime : -93,1375 € de l'Excel devient 93,14 €
        BigDecimal montant = saisie.montant().setScale(2, RoundingMode.HALF_UP);
        if (montant.signum() <= 0) throw new BusinessRuleException("Le montant arrondi au centime doit être d'au moins 0,01 €.");

        return new Resolue(compte, categorie, libelle, montant);
    }

    // Note vide : pas de note
    public static String nettoyerNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private static <T> boolean change(T ancien, Long nouvelId, Function<T, Long> id) {
        return !Objects.equals(ancien == null ? null : id.apply(ancien), nouvelId);
    }
}
