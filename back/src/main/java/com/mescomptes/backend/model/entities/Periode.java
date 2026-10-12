package com.mescomptes.backend.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// Les opérations ne référencent pas leur période : elle se retrouve par la date.
// La contiguïté (fin = veille du début suivant) est garantie par le service.
@Entity
@Table(name = "periode",
        uniqueConstraints = @UniqueConstraint(name = "uk_periode_debut", columnNames = {"utilisateur_id", "date_debut"}),
        check = @CheckConstraint(name = "ck_periode_dates", constraint = "date_fin >= date_debut"))
@Getter
@Setter
@NoArgsConstructor
public class Periode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    // Par défaut le mois de la date de fin (« Février 2026 »), modifiable
    @Column(nullable = false)
    private String nom;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;
}
