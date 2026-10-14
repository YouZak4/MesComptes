package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.ChangementEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChangementEmailRepository extends JpaRepository<ChangementEmail, Long> {
    Optional<ChangementEmail> findByUtilisateurId(Long utilisateurId);

    // Suppression en une seule requête, sans charger chaque ligne en mémoire
    @Modifying
    @Query("DELETE FROM ChangementEmail c WHERE c.dernierEnvoi < :limite")
    int deleteByDernierEnvoiBefore(@Param("limite") LocalDateTime limite);
}
