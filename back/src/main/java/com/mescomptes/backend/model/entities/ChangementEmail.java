package com.mescomptes.backend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Changement d'adresse mail en attente : la nouvelle adresse ne remplace l'ancienne
 * qu'une fois le code reçu sur la nouvelle saisi. Un seul changement en cours par utilisateur.
 */
@Entity
@Table(name = "changements_email")
// Pas de @Data : égalité par identité (entité JPA) et secrets exclus du toString
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChangementEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude // chargé à la demande
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;
    @Column(nullable = false)
    private String nouvelEmail;
    @ToString.Exclude
    @Column(nullable = false)
    private String codeHash;     // code haché, jamais en clair
    @Column(nullable = false)
    private LocalDateTime expiration;
    @Column(nullable = false)
    private int tentatives = 0;
    @Column(nullable = false)
    private LocalDateTime dernierEnvoi;
    // Adresse déjà liée à un autre compte : on fait comme si de rien n'était (pas de code envoyé),
    // pour que la réponse ne révèle pas qu'elle est inscrite
    @Column(nullable = false)
    private boolean emailDejaPris = false;
}
