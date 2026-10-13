package com.mescomptes.backend.model.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "utilisateurs")
// Ni @EqualsAndHashCode ni @Data : l'égalité par défaut (identité) est la bonne pour une entité JPA,
// un hashCode calculé sur les champs change quand l'id est attribué et parcourt les rôles chargés à la demande
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(unique = true, nullable = false)
    private String pseudo;

    @Setter
    @Getter
    @Column(unique = true, nullable = false)
    private String identifiant;

    @Setter
    @Getter
    @ToString.Exclude // le hash ne doit pas finir dans les logs
    @Column(nullable = false)
    private String motDePasse;

    @Setter
    @Getter
    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Getter
    @ToString.Exclude // chargés à la demande : afficher l'utilisateur hors transaction lèverait une exception
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "utilisateur_roles",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();
}
