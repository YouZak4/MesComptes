package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Masquage;
import com.mescomptes.backend.model.enums.TypeElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasquageRepository extends JpaRepository<Masquage, Long> {

    List<Masquage> findByUtilisateurIdAndTypeElement(Long utilisateurId, TypeElement typeElement);

    Optional<Masquage> findByUtilisateurIdAndTypeElementAndElementId(Long utilisateurId, TypeElement typeElement, Long elementId);
}
