package com.mescomptes.backend.model.entities;

import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.model.enums.TypeOperation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// Modèle d'opération ajoutée automatiquement au bon jour (loyer, salaire, abonnements…)
@Entity
@Table(name = "recurrence",
        check = {
                @CheckConstraint(name = "ck_recurrence_montant", constraint = "montant > 0"),
                @CheckConstraint(name = "ck_recurrence_jour", constraint = "jour BETWEEN 1 AND 31")
        })
@Getter
@Setter
@NoArgsConstructor
public class Recurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypeOperation type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libelle_id")
    private Libelle libelle;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequence frequence;

    // 1 à 31 ; un jour absent du mois (31 avril) devient le dernier jour du mois
    @Column(nullable = false)
    private int jour;

    @Column(nullable = false)
    private LocalDate dateDebut;

    // Facultative (fin d'un prêt) ; renseignée aussi quand la récurrence est arrêtée
    private LocalDate dateFin;

    // Prochaine occurrence à générer. Le générateur avance ce curseur, ce qui évite
    // de recréer une occurrence supprimée ou dont la date a été modifiée.
    @Column(nullable = false)
    private LocalDate prochaineOccurrence;
}
