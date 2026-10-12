package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    List<Compte> findByUtilisateurIdOrderByNom(Long utilisateurId);

    // Comptes proposés à la saisie
    List<Compte> findByUtilisateurIdAndArchiveFalseOrderByNom(Long utilisateurId);

    Optional<Compte> findByIdAndUtilisateurId(Long id, Long utilisateurId);
}
