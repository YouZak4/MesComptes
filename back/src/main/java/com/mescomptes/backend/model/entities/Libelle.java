package com.mescomptes.backend.model.entities;

import com.mescomptes.backend.model.Noms;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "libelle",
        uniqueConstraints = @UniqueConstraint(name = "uk_libelle_nom", columnNames = {"utilisateur_id", "nom_normalise"}))
@Getter
@Setter
@NoArgsConstructor
public class Libelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vide = libellé fourni, commun à tous les utilisateurs et non modifiable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String nom;

    // lower(trim(nom)) : sert à l'unicité sans tenir compte de la casse ni des espaces
    @Setter(AccessLevel.NONE)
    @Column(name = "nom_normalise", nullable = false)
    private String nomNormalise;

    // Proposée à la saisie, reste modifiable sur l'opération
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorieParDefaut;

    // Libellé personnel déjà utilisé : archivé plutôt que supprimé
    @Column(nullable = false)
    private boolean archive = false;

    public boolean isFourni() {
        return utilisateur == null;
    }

    @PrePersist
    @PreUpdate
    private void normaliserNom() {
        nom = Noms.nettoyer(nom);
        nomNormalise = Noms.normaliser(nom);
    }
}
