package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.model.dto.DtoOccurrence;
import com.mescomptes.backend.model.dto.DtoOperation;
import com.mescomptes.backend.model.dto.DtoPeriode;
import com.mescomptes.backend.model.dto.DtoTableauDeBord;
import com.mescomptes.backend.model.dto.DtoTotalCategorie;
import com.mescomptes.backend.model.dto.FiltreOperations;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Periode;
import com.mescomptes.backend.model.enums.TypeOperation;
import com.mescomptes.backend.repository.CategorieRepository;
import com.mescomptes.backend.repository.CompteRepository;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.PeriodeRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableauDeBordService {

    private static final int NB_DERNIERES_OPERATIONS = 10;

    private final PeriodeRepository periodeRepository;
    private final CompteRepository compteRepository;
    private final CategorieRepository categorieRepository;
    private final OperationRepository operationRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final PeriodeService periodeService;
    private final CompteService compteService;
    private final OperationService operationService;

    /**
     * Construit le tableau de bord d'une période.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param periodeId     période affichée ; vide : celle qui contient aujourd'hui, sinon la dernière
     * @param compteId      compte affiché ; vide : tous les comptes, archivés compris
     * @return totaux, totaux par catégorie, occurrences à venir, soldes prévus et dernières opérations
     */
    @Transactional(readOnly = true)
    public DtoTableauDeBord construire(UUID utilisateurId, Long periodeId, Long compteId) {
        Periode periode = periodeId != null ? periodeService.trouver(utilisateurId, periodeId) : periodeParDefaut(utilisateurId);
        if (compteId != null) compteService.trouver(utilisateurId, compteId);
        Optional<Periode> precedente = periodeRepository.findFirstByUtilisateurIdAndDateFinLessThanOrderByDateFinDesc(utilisateurId, periode.getDateDebut());
        Optional<Periode> suivante = periodeRepository.findFirstByUtilisateurIdAndDateDebutGreaterThanOrderByDateDebut(utilisateurId, periode.getDateDebut());

        // Totaux par catégorie de la période et de la précédente, pour le ou les comptes choisis
        Map<Long, BigDecimal> totaux = totauxParCategorie(utilisateurId, compteId, periode.getDateDebut(), periode.getDateFin());
        Map<Long, BigDecimal> totauxPrecedents = precedente
                .map(p -> totauxParCategorie(utilisateurId, compteId, p.getDateDebut(), p.getDateFin()))
                .orElse(Map.of());
        Map<Long, Categorie> categories = categorieRepository.findVisibles(utilisateurId).stream()
                .collect(Collectors.toMap(Categorie::getId, Function.identity()));

        Set<Long> categoriesAffichees = new HashSet<>(totaux.keySet());
        categoriesAffichees.addAll(totauxPrecedents.keySet());
        List<DtoTotalCategorie> parCategorie = categoriesAffichees.stream()
                .map(id -> {
                    Categorie categorie = categories.get(id);
                    BigDecimal total = totaux.getOrDefault(id, BigDecimal.ZERO);
                    BigDecimal totalPrecedent = totauxPrecedents.getOrDefault(id, BigDecimal.ZERO);
                    return new DtoTotalCategorie(id, categorie.getNom(), categorie.getType(), categorie.getIcone(), categorie.getCouleur(),
                            total, totalPrecedent, total.subtract(totalPrecedent));
                })
                .sorted(Comparator.comparing(DtoTotalCategorie::getTotal).reversed()
                        .thenComparing(Comparator.comparing(DtoTotalCategorie::getTotalPrecedent).reversed())
                        .thenComparing(DtoTotalCategorie::getNom))
                .toList();

        BigDecimal revenus = somme(parCategorie, TypeOperation.REVENU);
        BigDecimal depenses = somme(parCategorie, TypeOperation.DEPENSE);
        BigDecimal solde = revenus.subtract(depenses);

        List<DtoOccurrence> aVenir = occurrencesAVenir(utilisateurId, compteId, periode);
        BigDecimal netAVenir = aVenir.stream()
                .map(o -> o.getType() == TypeOperation.REVENU ? o.getMontant() : o.getMontant().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DtoOperation> dernieres = operationService
                .lister(utilisateurId, new FiltreOperations(periode.getId(), compteId, null, null, null)).stream()
                .limit(NB_DERNIERES_OPERATIONS)
                .toList();

        return new DtoTableauDeBord(
                new DtoPeriode(periode.getId(), periode.getNom(), periode.getDateDebut(), periode.getDateFin()),
                precedente.map(Periode::getId).orElse(null),
                suivante.map(Periode::getId).orElse(null),
                compteId,
                revenus, depenses, solde,
                parCategorie,
                aVenir,
                solde.add(netAVenir),
                soldeComptes(utilisateurId, compteId, periode.getDateFin()).add(netAVenir),
                dernieres);
    }

    private Periode periodeParDefaut(UUID utilisateurId) {
        return periodeRepository.findContenant(utilisateurId, LocalDate.now())
                .or(() -> periodeRepository.findFirstByUtilisateurIdOrderByDateFinDesc(utilisateurId))
                .orElseThrow(() -> new BusinessRuleException("Aucune période n'est encore définie."));
    }

    private Map<Long, BigDecimal> totauxParCategorie(UUID utilisateurId, Long compteId, LocalDate debut, LocalDate fin) {
        Map<Long, BigDecimal> totaux = new HashMap<>();
        for (Object[] ligne : operationRepository.totauxParCompteEtCategorie(utilisateurId, debut, fin)) {
            if (compteId == null || compteId.equals(ligne[0])) totaux.merge((Long) ligne[1], (BigDecimal) ligne[2], BigDecimal::add);
        }
        return totaux;
    }

    private static BigDecimal somme(List<DtoTotalCategorie> totaux, TypeOperation type) {
        return totaux.stream()
                .filter(t -> t.getType() == type)
                .map(DtoTotalCategorie::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Occurrences pas encore générées qui tombent dans la période, triées par date
    private List<DtoOccurrence> occurrencesAVenir(UUID utilisateurId, Long compteId, Periode periode) {
        return recurrenceRepository.findByUtilisateurIdOrderByJour(utilisateurId).stream()
                .filter(r -> compteId == null || compteId.equals(r.getCompte().getId()))
                .flatMap(r -> Occurrences.aGenererEntre(r, periode.getDateDebut(), periode.getDateFin()).stream()
                        .map(date -> new DtoOccurrence(r.getId(), date, r.getCompte().getId(), r.getType(), r.getMontant(),
                                r.getCategorie().getId(), r.getLibelle() == null ? null : r.getLibelle().getId(), r.getNote())))
                .sorted(Comparator.comparing(DtoOccurrence::getDate))
                .toList();
    }

    // Solde initial + mouvements jusqu'à la date, pour le ou les comptes choisis
    private BigDecimal soldeComptes(UUID utilisateurId, Long compteId, LocalDate jusqua) {
        Map<Long, BigDecimal> mouvements = operationRepository.sommeMouvementsParCompteJusqua(utilisateurId, TypeOperation.REVENU, jusqua).stream()
                .collect(Collectors.toMap(ligne -> (Long) ligne[0], ligne -> (BigDecimal) ligne[1]));
        return compteRepository.findByUtilisateurIdOrderByNom(utilisateurId).stream()
                .filter(c -> compteId == null || compteId.equals(c.getId()))
                .map(c -> c.getSoldeInitial().add(mouvements.getOrDefault(c.getId(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
