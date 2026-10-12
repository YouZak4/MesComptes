package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Libelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibelleRepository extends JpaRepository<Libelle, Long> {

    @Query("SELECT l FROM Libelle l WHERE l.utilisateur IS NULL")
    List<Libelle> findFournis();

    // Libellés fournis + personnels de l'utilisateur
    @Query("SELECT l FROM Libelle l WHERE l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId ORDER BY l.nom")
    List<Libelle> findVisibles(@Param("utilisateurId") Long utilisateurId);

    // Un libellé fourni ou appartenant à l'utilisateur
    @Query("SELECT l FROM Libelle l WHERE l.id = :id AND (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)")
    Optional<Libelle> findVisible(@Param("id") Long id, @Param("utilisateurId") Long utilisateurId);

    // Un nom ne doit exister ni parmi les fournis ni parmi les personnels de l'utilisateur
    @Query("SELECT COUNT(l) > 0 FROM Libelle l WHERE l.nomNormalise = :nomNormalise AND (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)")
    boolean existsNom(@Param("nomNormalise") String nomNormalise, @Param("utilisateurId") Long utilisateurId);

    // Autocomplétion : libellés visibles, non archivés, les plus utilisés par l'utilisateur d'abord
    @Query("""
            SELECT l FROM Libelle l
            LEFT JOIN Operation o ON o.libelle = l AND o.compte.utilisateur.id = :utilisateurId
            WHERE (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)
              AND l.archive = false
              AND l.nomNormalise LIKE CONCAT('%', LOWER(:q), '%')
            GROUP BY l
            ORDER BY COUNT(o) DESC, l.nom
            """)
    List<Libelle> rechercher(@Param("utilisateurId") Long utilisateurId, @Param("q") String q);
}
