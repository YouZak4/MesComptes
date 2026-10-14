package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Libelle;
import com.mescomptes.backend.model.entities.Operation;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

// Briques de filtre de la liste des opérations, combinées par OperationService
public final class OperationSpecifications {

    private OperationSpecifications() {
    }

    public static Specification<Operation> deLUtilisateur(UUID utilisateurId) {
        return (root, query, cb) -> cb.equal(root.get("compte").get("utilisateur").get("id"), utilisateurId);
    }

    public static Specification<Operation> entre(LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> cb.between(root.get("date"), debut, fin);
    }

    public static Specification<Operation> surLeCompte(Long compteId) {
        return (root, query, cb) -> cb.equal(root.get("compte").get("id"), compteId);
    }

    public static Specification<Operation> dansLaCategorie(Long categorieId) {
        return (root, query, cb) -> cb.equal(root.get("categorie").get("id"), categorieId);
    }

    public static Specification<Operation> avecLeLibelle(Long libelleId) {
        return (root, query, cb) -> cb.equal(root.get("libelle").get("id"), libelleId);
    }

    /**
     * Texte contenu dans la note ou le nom du libellé, sans tenir compte de la casse.
     *
     * @param motif texte normalisé, avec % et _ échappés par \
     */
    public static Specification<Operation> contenant(String motif) {
        return (root, query, cb) -> {
            Join<Operation, Libelle> libelle = root.join("libelle", JoinType.LEFT);
            String like = "%" + motif + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("note")), like, '\\'),
                    cb.like(libelle.get("nomNormalise"), like, '\\'));
        };
    }
}
