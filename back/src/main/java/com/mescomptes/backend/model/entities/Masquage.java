package com.mescomptes.backend.model.entities;

import com.mescomptes.backend.model.enums.TypeElement;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un utilisateur masque une catégorie ou un libellé fourni
@Entity
@Table(name = "masquage",
        uniqueConstraints = @UniqueConstraint(name = "uk_masquage", columnNames = {"utilisateur_id", "type_element", "element_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Masquage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_element", nullable = false, length = 10)
    private TypeElement typeElement;

    // Id d'une catégorie ou d'un libellé selon typeElement (pas de clé étrangère possible)
    @Column(name = "element_id", nullable = false)
    private Long elementId;
}
