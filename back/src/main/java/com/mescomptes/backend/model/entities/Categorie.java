package com.mescomptes.backend.model.entities;

import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.enums.TypeOperation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorie",
        uniqueConstraints = @UniqueConstraint(name = "uk_categorie_nom", columnNames = {"utilisateur_id", "nom_normalise"}))
@Getter
@Setter
@NoArgsConstructor
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vide = catégorie fournie, commune à tous les utilisateurs et non modifiable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 50)
    private String nom;

    // lower(trim(nom)) : sert à l'unicité sans tenir compte de la casse ni des espaces
    @Setter(AccessLevel.NONE)
    @Column(name = "nom_normalise", nullable = false)
    private String nomNormalise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypeOperation type;

    private String icone;

    // Couleur hexadécimale, ex. #4CAF50
    @Column(length = 7)
    private String couleur;

    // Catégorie personnelle déjà utilisée : archivée plutôt que supprimée
    @Column(nullable = false)
    private boolean archive = false;

    public boolean isFournie() {
        return utilisateur == null;
    }

    @PrePersist
    @PreUpdate
    private void normaliserNom() {
        nom = Noms.nettoyer(nom);
        nomNormalise = Noms.normaliser(nom);
    }
}
