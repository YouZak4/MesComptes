package com.mescomptes.backend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String nom;

    // Peut être négatif (compte à découvert au moment de sa création)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal soldeInitial = BigDecimal.ZERO;

    // Seules les opérations datées après cette date entrent dans le solde
    @Column(nullable = false)
    private LocalDate dateSoldeInitial;

    // Archivé : absent de la saisie, historique toujours consultable
    @Column(nullable = false)
    private boolean archive = false;
}
