package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    List<Compte> findByUtilisateurIdOrderByNom(UUID utilisateurId);

    // Comptes proposés à la saisie
    List<Compte> findByUtilisateurIdAndArchiveFalseOrderByNom(UUID utilisateurId);

    Optional<Compte> findByIdAndUtilisateurId(Long id, UUID utilisateurId);
}
