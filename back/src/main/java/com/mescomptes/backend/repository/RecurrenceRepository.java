package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

    List<Recurrence> findByUtilisateurIdOrderByJour(UUID utilisateurId);

    Optional<Recurrence> findByIdAndUtilisateurId(Long id, UUID utilisateurId);

    boolean existsByCategorieId(Long categorieId);

    boolean existsByLibelleId(Long libelleId);

    // Récurrences dont au moins une occurrence est arrivée à échéance (tâche planifiée)
    @Query("SELECT r FROM Recurrence r WHERE r.prochaineOccurrence <= :date AND (r.dateFin IS NULL OR r.prochaineOccurrence <= r.dateFin)")
    List<Recurrence> findAGenerer(@Param("date") LocalDate date);

    // Même chose pour un seul utilisateur (rattrapage à la connexion)
    @Query("SELECT r FROM Recurrence r WHERE r.utilisateur.id = :utilisateurId AND r.prochaineOccurrence <= :date AND (r.dateFin IS NULL OR r.prochaineOccurrence <= r.dateFin)")
    List<Recurrence> findAGenererPour(@Param("utilisateurId") UUID utilisateurId, @Param("date") LocalDate date);
}
