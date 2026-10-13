package com.mescomptes.backend.repository;

import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.enums.TypeOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor : filtres combinables de la liste (compte, catégorie, libellé, texte)
public interface OperationRepository extends JpaRepository<Operation, Long>, JpaSpecificationExecutor<Operation> {

    @Query("SELECT o FROM Operation o WHERE o.id = :id AND o.compte.utilisateur.id = :utilisateurId")
    Optional<Operation> findByIdAndUtilisateurId(@Param("id") Long id, @Param("utilisateurId") Long utilisateurId);

    // Revenus - dépenses de chaque compte de l'utilisateur, datés après la date du solde initial : [compteId, mouvement]
    @Query("""
            SELECT o.compte.id, SUM(CASE WHEN o.type = :revenu THEN o.montant ELSE -o.montant END)
            FROM Operation o
            WHERE o.compte.utilisateur.id = :utilisateurId AND o.date > o.compte.dateSoldeInitial
            GROUP BY o.compte.id
            """)
    List<Object[]> sommeMouvementsParCompte(@Param("utilisateurId") Long utilisateurId, @Param("revenu") TypeOperation revenu);

    // Même calcul, limité aux opérations datées jusqu'à une date incluse (solde d'un compte à une date)
    @Query("""
            SELECT o.compte.id, SUM(CASE WHEN o.type = :revenu THEN o.montant ELSE -o.montant END)
            FROM Operation o
            WHERE o.compte.utilisateur.id = :utilisateurId AND o.date > o.compte.dateSoldeInitial AND o.date <= :jusqua
            GROUP BY o.compte.id
            """)
    List<Object[]> sommeMouvementsParCompteJusqua(@Param("utilisateurId") Long utilisateurId, @Param("revenu") TypeOperation revenu,
                                                   @Param("jusqua") LocalDate jusqua);

    // Totaux des opérations de l'utilisateur entre deux dates incluses : [compteId, categorieId, total]
    @Query("""
            SELECT o.compte.id, o.categorie.id, SUM(o.montant)
            FROM Operation o
            WHERE o.compte.utilisateur.id = :utilisateurId AND o.date BETWEEN :debut AND :fin
            GROUP BY o.compte.id, o.categorie.id
            """)
    List<Object[]> totauxParCompteEtCategorie(@Param("utilisateurId") Long utilisateurId,
                                              @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    // Dernière opération saisie par l'utilisateur (id le plus grand), pour proposer son compte par défaut
    Optional<Operation> findFirstByCompte_Utilisateur_IdOrderByIdDesc(Long utilisateurId);

    boolean existsByRecurrenceIdAndDate(Long recurrenceId, LocalDate date);

    boolean existsByRecurrenceId(Long recurrenceId);

    boolean existsByCompteId(Long compteId);

    boolean existsByCategorieId(Long categorieId);

    boolean existsByLibelleId(Long libelleId);
}
