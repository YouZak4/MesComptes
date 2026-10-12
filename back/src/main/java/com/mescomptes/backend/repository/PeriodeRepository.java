package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Periode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodeRepository extends JpaRepository<Periode, Long> {

    List<Periode> findByUtilisateurIdOrderByDateDebut(Long utilisateurId);

    Optional<Periode> findByIdAndUtilisateurId(Long id, Long utilisateurId);

    // Période contenant une date donnée (chaque jour appartient à exactement une période)
    @Query("SELECT p FROM Periode p WHERE p.utilisateur.id = :utilisateurId AND :date BETWEEN p.dateDebut AND p.dateFin")
    Optional<Periode> findContenant(@Param("utilisateurId") Long utilisateurId, @Param("date") LocalDate date);

    Optional<Periode> findFirstByUtilisateurIdOrderByDateFinDesc(Long utilisateurId);

    // Voisines, pour maintenir la contiguïté quand une limite bouge
    Optional<Periode> findFirstByUtilisateurIdAndDateDebutGreaterThanOrderByDateDebut(Long utilisateurId, LocalDate date);

    Optional<Periode> findFirstByUtilisateurIdAndDateFinLessThanOrderByDateFinDesc(Long utilisateurId, LocalDate date);
}
