package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.dto.DtoOperation;
import com.mescomptes.backend.model.dto.DtoOperationSaisie;
import com.mescomptes.backend.model.dto.FiltreOperations;
import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Periode;
import com.mescomptes.backend.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mescomptes.backend.repository.OperationSpecifications.*;

@Service
@RequiredArgsConstructor
public class OperationService {

    private static final String OPERATION_INTROUVABLE = "Cette opération n'existe pas.";
    private static final Sort PLUS_RECENTES_D_ABORD = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));

    private final OperationRepository operationRepository;
    private final SaisieOperationResolver resolver;
    private final PeriodeService periodeService;
    private final CompteService compteService;
    private final CategorieService categorieService;
    private final LibelleService libelleService;

    /**
     * Retourne les opérations de l'utilisateur, les plus récentes d'abord, selon des filtres facultatifs et combinables.
     * Chaque id de filtre doit désigner un élément de l'utilisateur (ou fourni) : 404 sinon, comme les autres routes.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param filtre        période, compte, catégorie, libellé et texte (note ou libellé)
     * @return la liste filtrée
     */
    @Transactional(readOnly = true)
    public List<DtoOperation> lister(UUID utilisateurId, FiltreOperations filtre) {
        List<Specification<Operation>> criteres = new ArrayList<>();
        criteres.add(deLUtilisateur(utilisateurId));
        if (filtre.periodeId() != null) {
            Periode periode = periodeService.trouver(utilisateurId, filtre.periodeId());
            criteres.add(entre(periode.getDateDebut(), periode.getDateFin()));
        }
        if (filtre.compteId() != null) {
            compteService.trouver(utilisateurId, filtre.compteId());
            criteres.add(surLeCompte(filtre.compteId()));
        }
        if (filtre.categorieId() != null) {
            categorieService.trouverVisible(utilisateurId, filtre.categorieId());
            criteres.add(dansLaCategorie(filtre.categorieId()));
        }
        if (filtre.libelleId() != null) {
            libelleService.trouverVisible(utilisateurId, filtre.libelleId());
            criteres.add(avecLeLibelle(filtre.libelleId()));
        }
        if (filtre.q() != null && !filtre.q().isBlank()) criteres.add(contenant(Noms.motifRecherche(filtre.q())));

        return operationRepository.findAll(Specification.allOf(criteres), PLUS_RECENTES_D_ABORD).stream()
                .map(this::versDto)
                .toList();
    }

    /**
     * Enregistre une opération. Si sa date est après la dernière période, les périodes manquantes sont créées.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           compte, date, montant, catégorie et/ou libellé, note
     * @return l'opération créée
     */
    @Transactional
    public DtoOperation creer(UUID utilisateurId, DtoOperationSaisie dto) {
        Operation operation = new Operation();
        appliquer(utilisateurId, operation, dto);
        periodeService.couvrirJusqua(utilisateurId, operation.getDate());
        return versDto(operationRepository.save(operation));
    }

    /**
     * Modifie une opération. Une opération générée par une récurrence garde son lien avec elle,
     * et seule cette occurrence change.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de l'opération
     * @param dto           nouvelles valeurs
     * @return l'opération modifiée
     */
    @Transactional
    public DtoOperation modifier(UUID utilisateurId, Long id, DtoOperationSaisie dto) {
        Operation operation = trouver(utilisateurId, id);
        appliquer(utilisateurId, operation, dto);
        periodeService.couvrirJusqua(utilisateurId, operation.getDate());
        try {
            return versDto(operationRepository.saveAndFlush(operation));
        } catch (DataIntegrityViolationException e) {
            // Unicité (récurrence, date) : une autre occurrence de la même récurrence existe déjà à cette date
            throw new BusinessRuleException("Cette récurrence a déjà une opération à cette date.");
        }
    }

    /**
     * Supprime une opération. Une occurrence de récurrence supprimée n'est pas recréée.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de l'opération
     */
    @Transactional
    public void supprimer(UUID utilisateurId, Long id) {
        operationRepository.delete(trouver(utilisateurId, id));
    }

    /**
     * Retourne une opération de l'utilisateur, ou une 404.
     */
    public Operation trouver(UUID utilisateurId, Long id) {
        return operationRepository.findByIdAndUtilisateurId(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(OPERATION_INTROUVABLE));
    }

    // Applique la saisie à l'opération selon les règles communes (voir SaisieOperationResolver)
    private void appliquer(UUID utilisateurId, Operation operation, DtoOperationSaisie dto) {
        SaisieOperationResolver.Resolue existant = operation.getId() == null ? null
                : new SaisieOperationResolver.Resolue(operation.getCompte(), operation.getCategorie(), operation.getLibelle(), operation.getMontant());
        SaisieOperationResolver.Resolue saisie = resolver.resoudre(utilisateurId,
                new SaisieOperationResolver.Saisie(dto.getCompteId(), dto.getCategorieId(), dto.getLibelleId(), dto.getMontant()), existant);

        operation.setCompte(saisie.compte());
        operation.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        operation.setType(saisie.categorie().getType());
        operation.setMontant(saisie.montant());
        operation.setCategorie(saisie.categorie());
        operation.setLibelle(saisie.libelle());
        operation.setNote(SaisieOperationResolver.nettoyerNote(dto.getNote()));
    }
    private DtoOperation versDto(Operation operation) {
        return new DtoOperation(operation.getId(), operation.getCompte().getId(), operation.getDate(), operation.getType(),
                operation.getMontant(), operation.getCategorie().getId(),
                operation.getLibelle() == null ? null : operation.getLibelle().getId(), operation.getNote(),
                operation.getRecurrence() == null ? null : operation.getRecurrence().getId());
    }
}
