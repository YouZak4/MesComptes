package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Masquage;
import com.mescomptes.backend.model.enums.TypeElement;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MasquageRepository extends JpaRepository<Masquage, Long> {

    // Ids des éléments d'un type que l'utilisateur a masqués
    @Query("SELECT m.elementId FROM Masquage m WHERE m.utilisateur.id = :utilisateurId AND m.typeElement = :type")
    Set<Long> findIdsMasques(@Param("utilisateurId") UUID utilisateurId, @Param("type") TypeElement type);

    Optional<Masquage> findByUtilisateurIdAndTypeElementAndElementId(UUID utilisateurId, TypeElement typeElement, Long elementId);
}
