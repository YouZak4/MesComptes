package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Libelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibelleRepository extends JpaRepository<Libelle, Long> {

    @Query("SELECT l FROM Libelle l WHERE l.utilisateur IS NULL")
    List<Libelle> findFournis();

    // Libellés fournis + personnels de l'utilisateur
    @Query("SELECT l FROM Libelle l WHERE l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId ORDER BY l.nom")
    List<Libelle> findVisibles(@Param("utilisateurId") UUID utilisateurId);

    // Un libellé fourni ou appartenant à l'utilisateur
    @Query("SELECT l FROM Libelle l WHERE l.id = :id AND (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)")
    Optional<Libelle> findVisible(@Param("id") Long id, @Param("utilisateurId") UUID utilisateurId);

    // Libellés visibles portant ce nom : un nom ne doit exister ni parmi les fournis ni parmi les personnels
    @Query("SELECT l FROM Libelle l WHERE l.nomNormalise = :nomNormalise AND (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)")
    List<Libelle> findVisiblesParNom(@Param("nomNormalise") String nomNormalise, @Param("utilisateurId") UUID utilisateurId);

    /**
     * Autocomplétion : libellés visibles et non archivés contenant le texte, les plus utilisés par l'utilisateur d'abord.
     *
     * @param motif texte déjà normalisé, avec % et _ échappés par \
     */
    @Query("""
            SELECT l FROM Libelle l
            WHERE (l.utilisateur IS NULL OR l.utilisateur.id = :utilisateurId)
              AND l.archive = false
              AND l.nomNormalise LIKE CONCAT('%', :motif, '%') ESCAPE '\\'
            ORDER BY (SELECT COUNT(o) FROM Operation o WHERE o.libelle = l AND o.compte.utilisateur.id = :utilisateurId) DESC, l.nom
            """)
    List<Libelle> rechercher(@Param("utilisateurId") UUID utilisateurId, @Param("motif") String motif);

    boolean existsByCategorieParDefautId(Long categorieId);
}
