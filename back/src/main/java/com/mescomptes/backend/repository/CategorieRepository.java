package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    @Query("SELECT c FROM Categorie c WHERE c.utilisateur IS NULL")
    List<Categorie> findFournies();

    // Catégories fournies + personnelles de l'utilisateur
    @Query("SELECT c FROM Categorie c WHERE c.utilisateur IS NULL OR c.utilisateur.id = :utilisateurId ORDER BY c.type, c.nom")
    List<Categorie> findVisibles(@Param("utilisateurId") UUID utilisateurId);

    // Une catégorie fournie ou appartenant à l'utilisateur
    @Query("SELECT c FROM Categorie c WHERE c.id = :id AND (c.utilisateur IS NULL OR c.utilisateur.id = :utilisateurId)")
    Optional<Categorie> findVisible(@Param("id") Long id, @Param("utilisateurId") UUID utilisateurId);

    // Catégories visibles portant ce nom : un nom ne doit exister ni parmi les fournies ni parmi les personnelles
    @Query("SELECT c FROM Categorie c WHERE c.nomNormalise = :nomNormalise AND (c.utilisateur IS NULL OR c.utilisateur.id = :utilisateurId)")
    List<Categorie> findVisiblesParNom(@Param("nomNormalise") String nomNormalise, @Param("utilisateurId") UUID utilisateurId);
}
