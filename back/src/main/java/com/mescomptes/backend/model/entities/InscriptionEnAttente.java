package com.mescomptes.backend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscriptions_en_attente")
// Pas de @Data : égalité par identité (entité JPA) et secrets exclus du toString
@Getter
@Setter
@ToString
@NoArgsConstructor
public class InscriptionEnAttente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant opaque transmis au front à la place de l'e-mail
    @ToString.Exclude // avec le code, il suffit à valider l'inscription
    @Column(unique = true, nullable = false)
    private String jeton;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String nom;
    private String prenom;
    @Column(nullable = false)
    private String identifiant;
    @ToString.Exclude
    @Column(nullable = false)
    private String motDePasse;   // déjà haché
    @ToString.Exclude
    @Column(nullable = false)
    private String codeHash;     // code haché, jamais en clair
    @Column(nullable = false)
    private LocalDateTime expiration;
    @Column(nullable = false)
    private int tentatives = 0;
    @Column(nullable = false)
    private LocalDateTime dernierEnvoi;
    // E-mail déjà lié à un compte : on simule une inscription normale mais on envoie une alerte au lieu d'un code
    @Column(nullable = false)
    private boolean compteExistant = false;
}