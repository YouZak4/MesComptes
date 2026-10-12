package com.mescomptes.backend.model.entities;

import com.mescomptes.backend.model.enums.TypeOperation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// L'utilisateur propriétaire se retrouve via le compte ; la période via la date
@Entity
@Table(name = "operation",
        indexes = @Index(name = "idx_operation_compte_date", columnList = "compte_id, date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_operation_recurrence_date", columnNames = {"recurrence_id", "date"}),
        check = @CheckConstraint(name = "ck_operation_montant", constraint = "montant > 0"))
@Getter
@Setter
@NoArgsConstructor
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(nullable = false)
    private LocalDate date;

    // Déduit de la catégorie ; donne le sens du montant
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TypeOperation type;

    // Toujours positif, arrondi au centime
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "libelle_id")
    private Libelle libelle;

    private String note;

    // Renseignée si l'opération a été générée par une récurrence
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_id")
    private Recurrence recurrence;
}
