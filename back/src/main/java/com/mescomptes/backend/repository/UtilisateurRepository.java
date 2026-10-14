package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdentifiant(String identifiant);

    @EntityGraph(attributePaths = "roles")
    Optional<Utilisateur> findByIdentifiant(String identifiant);
}


