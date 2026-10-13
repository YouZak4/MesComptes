package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.dto.DtoRecurrence;
import com.mescomptes.backend.model.dto.DtoRecurrenceSaisie;
import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecurrenceService {

    private static final String RECURRENCE_INTROUVABLE = "Cette récurrence n'existe pas.";

    private final RecurrenceRepository recurrenceRepository;
    private final OperationRepository operationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SaisieOperationResolver resolver;
    private final OperationService operationService;
    private final GenerateurRecurrences generateur;

    /**
     * Retourne les récurrences de l'utilisateur, arrêtées comprises, triées par jour du mois.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return la liste des récurrences
     */
    @Transactional(readOnly = true)
    public List<DtoRecurrence> lister(Long utilisateurId) {
        return recurrenceRepository.findByUtilisateurIdOrderByJour(utilisateurId).stream().map(this::versDto).toList();
    }

    /**
     * Crée une récurrence. Les occurrences déjà échues depuis la date de début sont générées tout de suite.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           champs de l'opération et calendrier
     * @return la récurrence créée
     */
    @Transactional
    public DtoRecurrence creer(Long utilisateurId, DtoRecurrenceSaisie dto) {
        Recurrence recurrence = new Recurrence();
        recurrence.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        appliquer(utilisateurId, recurrence, dto);
        recurrence.setProchaineOccurrence(Occurrences.premiereAPartirDe(recurrence, recurrence.getDateDebut()));
        return enregistrerEtGenerer(recurrence);
    }

    /**
     * Modifie une récurrence. Seules les occurrences futures changent : les opérations déjà générées restent telles quelles.
     * Si le calendrier change, ou si une récurrence arrêtée repart, les occurrences reprennent à partir de demain
     * (pas de rattrapage des dates passées).
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de la récurrence
     * @param dto           nouvelles valeurs
     * @return la récurrence modifiée
     */
    @Transactional
    public DtoRecurrence modifier(Long utilisateurId, Long id, DtoRecurrenceSaisie dto) {
        Recurrence recurrence = trouver(utilisateurId, id);
        boolean etaitActive = estActive(recurrence);
        Frequence ancienneFrequence = recurrence.getFrequence();
        int ancienJour = recurrence.getJour();
        LocalDate ancienDebut = recurrence.getDateDebut();
        LocalDate ancienneProchaine = recurrence.getProchaineOccurrence();

        appliquer(utilisateurId, recurrence, dto);

        boolean calendrierChange = ancienneFrequence != recurrence.getFrequence() || ancienJour != recurrence.getJour()
                || !Objects.equals(ancienDebut, recurrence.getDateDebut());
        // Récurrence arrêtée dont la date de fin a été retirée ou repoussée
        boolean reprend = !etaitActive && (recurrence.getDateFin() == null || !recurrence.getDateFin().isBefore(LocalDate.now()));
        if (calendrierChange || reprend) {
            LocalDate demain = LocalDate.now().plusDays(1);
            // Occurrences déjà générées jusqu'à aujourd'hui : on repart de demain, ou de la prochaine si elle est plus tôt (retard)
            LocalDate aPartirDe = reprend || ancienneProchaine.isAfter(demain) ? demain : ancienneProchaine;
            if (aPartirDe.isBefore(recurrence.getDateDebut())) aPartirDe = recurrence.getDateDebut();
            recurrence.setProchaineOccurrence(Occurrences.premiereAPartirDe(recurrence, aPartirDe));
        }
        return enregistrerEtGenerer(recurrence);
    }

    /**
     * Arrête une récurrence : plus aucune occurrence après aujourd'hui, l'historique reste.
     * Une récurrence qui n'a encore rien généré est simplement supprimée.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de la récurrence
     * @return la récurrence arrêtée, ou vide si elle a été supprimée
     */
    @Transactional
    public Optional<DtoRecurrence> supprimer(Long utilisateurId, Long id) {
        Recurrence recurrence = trouver(utilisateurId, id);
        if (!operationRepository.existsByRecurrenceId(id)) {
            recurrenceRepository.delete(recurrence);
            return Optional.empty();
        }
        LocalDate aujourdHui = LocalDate.now();
        if (recurrence.getDateFin() == null || recurrence.getDateFin().isAfter(aujourdHui)) recurrence.setDateFin(aujourdHui);
        return Optional.of(versDto(recurrence));
    }

    /**
     * « Répéter chaque mois » : transforme une opération saisie à la main en récurrence qui commence à sa date.
     * L'opération devient la première occurrence.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param operationId   id de l'opération
     * @param frequence     mensuelle ou annuelle
     * @return la récurrence créée
     */
    @Transactional
    public DtoRecurrence repeter(Long utilisateurId, Long operationId, Frequence frequence) {
        Operation operation = operationService.trouver(utilisateurId, operationId);
        if (operation.getRecurrence() != null) throw new BusinessRuleException("Cette opération fait déjà partie d'une récurrence.");
        SaisieOperationResolver.Resolue saisie = resolver.resoudre(utilisateurId, new SaisieOperationResolver.Saisie(
                operation.getCompte().getId(), operation.getCategorie().getId(),
                operation.getLibelle() == null ? null : operation.getLibelle().getId(), operation.getMontant()), null);

        Recurrence recurrence = new Recurrence();
        recurrence.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        recurrence.setCompte(saisie.compte());
        recurrence.setType(saisie.categorie().getType());
        recurrence.setMontant(saisie.montant());
        recurrence.setCategorie(saisie.categorie());
        recurrence.setLibelle(saisie.libelle());
        recurrence.setNote(operation.getNote());
        recurrence.setFrequence(frequence);
        recurrence.setJour(operation.getDate().getDayOfMonth());
        recurrence.setDateDebut(operation.getDate());
        recurrence.setProchaineOccurrence(Occurrences.suivante(recurrence, operation.getDate()));
        operation.setRecurrence(recurrence);
        return enregistrerEtGenerer(recurrence);
    }

    private Recurrence trouver(Long utilisateurId, Long id) {
        return recurrenceRepository.findByIdAndUtilisateurId(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(RECURRENCE_INTROUVABLE));
    }

    private void appliquer(Long utilisateurId, Recurrence recurrence, DtoRecurrenceSaisie dto) {
        if (dto.getDateFin() != null && dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new BusinessRuleException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        SaisieOperationResolver.Resolue existant = recurrence.getId() == null ? null
                : new SaisieOperationResolver.Resolue(recurrence.getCompte(), recurrence.getCategorie(), recurrence.getLibelle(), recurrence.getMontant());
        SaisieOperationResolver.Resolue saisie = resolver.resoudre(utilisateurId,
                new SaisieOperationResolver.Saisie(dto.getCompteId(), dto.getCategorieId(), dto.getLibelleId(), dto.getMontant()), existant);

        recurrence.setCompte(saisie.compte());
        recurrence.setType(saisie.categorie().getType());
        recurrence.setMontant(saisie.montant());
        recurrence.setCategorie(saisie.categorie());
        recurrence.setLibelle(saisie.libelle());
        recurrence.setNote(SaisieOperationResolver.nettoyerNote(dto.getNote()));
        recurrence.setFrequence(dto.getFrequence());
        recurrence.setJour(dto.getJour() != null ? dto.getJour() : dto.getDateDebut().getDayOfMonth());
        recurrence.setDateDebut(dto.getDateDebut());
        recurrence.setDateFin(dto.getDateFin());
    }

    // Génère tout de suite les occurrences déjà échues, pour que l'utilisateur les voie sans attendre la nuit
    private DtoRecurrence enregistrerEtGenerer(Recurrence recurrence) {
        recurrence = recurrenceRepository.saveAndFlush(recurrence);
        generateur.generer(recurrence.getId(), LocalDate.now());
        return versDto(recurrence);
    }

    private static boolean estActive(Recurrence recurrence) {
        return recurrence.getDateFin() == null || !recurrence.getProchaineOccurrence().isAfter(recurrence.getDateFin());
    }

    private DtoRecurrence versDto(Recurrence recurrence) {
        return new DtoRecurrence(recurrence.getId(), recurrence.getCompte().getId(), recurrence.getType(), recurrence.getMontant(),
                recurrence.getCategorie().getId(), recurrence.getLibelle() == null ? null : recurrence.getLibelle().getId(),
                recurrence.getNote(), recurrence.getFrequence(), recurrence.getJour(), recurrence.getDateDebut(),
                recurrence.getDateFin(), recurrence.getProchaineOccurrence(), estActive(recurrence));
    }
}
