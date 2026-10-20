package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.dto.DtoPeriode;
import com.mescomptes.backend.model.dto.DtoPeriodeSaisie;
import com.mescomptes.backend.model.entities.Periode;
import com.mescomptes.backend.repository.PeriodeRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Les périodes d'un utilisateur se suivent sans trou ni chevauchement :
 * la fin d'une période est toujours la veille du début de la suivante.
 */
@Service
@RequiredArgsConstructor
public class PeriodeService {

    private static final DateTimeFormatter FORMAT_NOM = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PERIODE_INTROUVABLE = "Cette période n'existe pas.";
    private static final String DATES_INCOHERENTES = "La date de fin doit être postérieure ou égale à la date de début.";

    private final PeriodeRepository periodeRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Retourne les périodes de l'utilisateur, de la plus ancienne à la plus récente.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return la liste des périodes (vide à la première connexion)
     */
    @Transactional(readOnly = true)
    public List<DtoPeriode> lister(UUID utilisateurId) {
        return periodeRepository.findByUtilisateurIdOrderByDateDebut(utilisateurId).stream().map(this::versDto).toList();
    }

    /**
     * Crée une période.
     * <ul>
     *     <li>Première période : la date de début est obligatoire.</li>
     *     <li>Sinon, la période s'ajoute après la dernière (début = lendemain de sa fin, date de début facultative)
     *     ou avant la première (fin = veille de son début).</li>
     * </ul>
     * Sans date de fin, la période dure la durée par défaut (un mois).
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           nom et dates souhaités
     * @return la période créée
     */
    @Transactional
    public DtoPeriode creer(UUID utilisateurId, DtoPeriodeSaisie dto) {
        Optional<Periode> derniere = periodeRepository.findFirstByUtilisateurIdOrderByDateFinDesc(utilisateurId);
        LocalDate debut;
        LocalDate fin;

        if (derniere.isEmpty()) {
            if (dto.getDateDebut() == null) throw new BusinessRuleException("La date de début de la première période est obligatoire.");
            debut = dto.getDateDebut();
            fin = dto.getDateFin() != null ? dto.getDateFin() : finParDefaut(debut);
        } else {
            LocalDate lendemainDerniere = derniere.get().getDateFin().plusDays(1);
            LocalDate veillePremiere = periodeRepository.findByUtilisateurIdOrderByDateDebut(utilisateurId).getFirst().getDateDebut().minusDays(1);

            if (dto.getDateDebut() == null || dto.getDateDebut().equals(lendemainDerniere)) {
                debut = lendemainDerniere;
                fin = dto.getDateFin() != null ? dto.getDateFin() : finParDefaut(debut);
            } else if (veillePremiere.equals(dto.getDateFin())) {
                debut = dto.getDateDebut();
                fin = dto.getDateFin();
            } else {
                throw new BusinessRuleException("Une nouvelle période doit commencer le " + lendemainDerniere.format(FORMAT_DATE)
                        + " (lendemain de la dernière) ou se terminer le " + veillePremiere.format(FORMAT_DATE) + " (veille de la première).");
            }
        }
        if (fin.isBefore(debut)) throw new BusinessRuleException(DATES_INCOHERENTES);

        return versDto(enregistrer(utilisateurId, nomOuDefaut(dto.getNom(), fin), debut, fin));
    }

    /**
     * Modifie une période. Déplacer une de ses limites déplace celle de la période voisine,
     * qui doit garder au moins un jour.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de la période
     * @param dto           nouvelles dates (obligatoires) et nom (vide = nom par défaut)
     * @return la période modifiée
     */
    @Transactional
    public DtoPeriode modifier(UUID utilisateurId, Long id, DtoPeriodeSaisie dto) {
        if (dto.getDateDebut() == null || dto.getDateFin() == null) throw new BusinessRuleException("Les dates de début et de fin sont obligatoires.");
        if (dto.getDateFin().isBefore(dto.getDateDebut())) throw new BusinessRuleException(DATES_INCOHERENTES);

        Periode periode = trouver(utilisateurId, id);

        if (!dto.getDateDebut().equals(periode.getDateDebut())) {
            periodeRepository.findFirstByUtilisateurIdAndDateFinLessThanOrderByDateFinDesc(utilisateurId, periode.getDateDebut())
                    .ifPresent(precedente -> {
                        LocalDate nouvelleFin = dto.getDateDebut().minusDays(1);
                        if (nouvelleFin.isBefore(precedente.getDateDebut())) {
                            throw new BusinessRuleException("La période « " + precedente.getNom() + " » doit garder au moins un jour : "
                                    + "la date de début doit être après le " + precedente.getDateDebut().format(FORMAT_DATE) + ".");
                        }
                        // Le nom suit la date de fin s'il n'avait pas été personnalisé
                        if (precedente.getNom().equals(nomParDefaut(precedente.getDateFin()))) precedente.setNom(nomParDefaut(nouvelleFin));
                        precedente.setDateFin(nouvelleFin);
                    });
        }
        if (!dto.getDateFin().equals(periode.getDateFin())) {
            periodeRepository.findFirstByUtilisateurIdAndDateDebutGreaterThanOrderByDateDebut(utilisateurId, periode.getDateDebut())
                    .ifPresent(suivante -> {
                        LocalDate nouveauDebut = dto.getDateFin().plusDays(1);
                        if (nouveauDebut.isAfter(suivante.getDateFin())) {
                            throw new BusinessRuleException("La période « " + suivante.getNom() + " » doit garder au moins un jour : "
                                    + "la date de fin doit être avant le " + suivante.getDateFin().format(FORMAT_DATE) + ".");
                        }
                        suivante.setDateDebut(nouveauDebut);
                    });
        }
        // Voisines d'abord : sinon le nouveau début pourrait croiser l'ancien début de la suivante (contrainte unique)
        periodeRepository.flush();

        periode.setNom(nomOuDefaut(dto.getNom(), dto.getDateFin()));
        periode.setDateDebut(dto.getDateDebut());
        periode.setDateFin(dto.getDateFin());
        return versDto(periode);
    }

    /**
     * Crée les périodes manquantes pour qu'une date soit couverte, avec la durée par défaut :
     * après la dernière période si la date est plus tard, avant la première si elle est plus tôt.
     * Appelé à l'enregistrement d'une opération.
     *
     * @param utilisateurId id de l'utilisateur
     * @param date          date à couvrir
     */
    @Transactional
    public void couvrir(UUID utilisateurId, LocalDate date) {
        Optional<Periode> derniere = periodeRepository.findFirstByUtilisateurIdOrderByDateFinDesc(utilisateurId);
        // Aucune période (assistant de première connexion non terminé) : on part du mois de la date
        LocalDate fin = derniere.map(Periode::getDateFin).orElse(date.withDayOfMonth(1).minusDays(1));

        while (fin.isBefore(date)) {
            LocalDate debut = fin.plusDays(1);
            fin = finParDefaut(debut);
            enregistrer(utilisateurId, nomParDefaut(fin), debut, fin);
        }

        LocalDate debut = periodeRepository.findFirstByUtilisateurIdOrderByDateDebut(utilisateurId).orElseThrow().getDateDebut();
        while (debut.isAfter(date)) {
            LocalDate finPrecedente = debut.minusDays(1);
            debut = debutParDefaut(finPrecedente);
            enregistrer(utilisateurId, nomParDefaut(finPrecedente), debut, finPrecedente);
        }
    }

    /**
     * Retourne une période de l'utilisateur, ou une 404 si elle n'existe pas ou appartient à un autre utilisateur.
     */
    @Transactional(readOnly = true)
    public Periode trouver(UUID utilisateurId, Long id) {
        return periodeRepository.findByIdAndUtilisateurId(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(PERIODE_INTROUVABLE));
    }

    private Periode enregistrer(UUID utilisateurId, String nom, LocalDate debut, LocalDate fin) {
        Periode periode = new Periode();
        periode.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        periode.setNom(nom);
        periode.setDateDebut(debut);
        periode.setDateFin(fin);
        return periodeRepository.save(periode);
    }

    // Durée par défaut : un mois (réglage utilisateur prévu, fixe en V1)
    private static LocalDate finParDefaut(LocalDate debut) {
        return debut.plusMonths(1).minusDays(1);
    }

    // Symétrique de finParDefaut, pour une période ajoutée avant la première : fin 28/02 → début 01/02
    private static LocalDate debutParDefaut(LocalDate fin) {
        return fin.plusDays(1).minusMonths(1);
    }

    private static String nomOuDefaut(String nom, LocalDate fin) {
        return nom == null || nom.isBlank() ? nomParDefaut(fin) : Noms.nettoyer(nom);
    }

    // Mois de la date de fin : 23/01 → 25/02 donne « Février 2026 »
    private static String nomParDefaut(LocalDate fin) {
        String nom = fin.format(FORMAT_NOM);
        return nom.substring(0, 1).toUpperCase(Locale.FRENCH) + nom.substring(1);
    }

    private DtoPeriode versDto(Periode periode) {
        return new DtoPeriode(periode.getId(), periode.getNom(), periode.getDateDebut(), periode.getDateFin());
    }
}
