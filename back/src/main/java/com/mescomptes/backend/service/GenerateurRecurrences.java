package com.mescomptes.backend.service;

import com.mescomptes.backend.model.entities.Operation;
import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Crée les opérations des récurrences dont la date est arrivée.
 * Chaque occurrence n'est créée qu'une fois : le curseur prochaineOccurrence avance après chaque génération
 * (une occurrence supprimée n'est donc pas recréée), et l'unicité (récurrence, date) protège d'un double passage.
 */
@Service
@RequiredArgsConstructor
public class GenerateurRecurrences {

    private final RecurrenceRepository recurrenceRepository;
    private final OperationRepository operationRepository;
    private final PeriodeService periodeService;

    /**
     * Génère les occurrences d'une récurrence jusqu'à une date incluse.
     * Appelée hors transaction, chaque récurrence est traitée dans sa propre transaction.
     *
     * @param recurrenceId id de la récurrence
     * @param jusqua       date limite, en général aujourd'hui
     * @return le nombre d'opérations créées
     */
    @Transactional
    public int generer(Long recurrenceId, LocalDate jusqua) {
        Recurrence recurrence = recurrenceRepository.findById(recurrenceId).orElseThrow();
        List<LocalDate> dates = Occurrences.aGenererEntre(recurrence, recurrence.getProchaineOccurrence(), jusqua);
        int creees = 0;

        for (LocalDate date : dates) {
            if (!operationRepository.existsByRecurrenceIdAndDate(recurrenceId, date)) {
                operationRepository.save(occurrence(recurrence, date));
                creees++;
            }
            recurrence.setProchaineOccurrence(Occurrences.suivante(recurrence, date));
        }
        if (!dates.isEmpty()) {
            UUID utilisateurId = recurrence.getUtilisateur().getId();
            // D'abord la première date : sans période existante, la première créée doit la contenir
            periodeService.couvrirJusqua(utilisateurId, dates.getFirst());
            periodeService.couvrirJusqua(utilisateurId, dates.getLast());
        }
        return creees;
    }

    private static Operation occurrence(Recurrence recurrence, LocalDate date) {
        Operation operation = new Operation();
        operation.setCompte(recurrence.getCompte());
        operation.setDate(date);
        operation.setType(recurrence.getType());
        operation.setMontant(recurrence.getMontant());
        operation.setCategorie(recurrence.getCategorie());
        operation.setLibelle(recurrence.getLibelle());
        operation.setNote(recurrence.getNote());
        operation.setRecurrence(recurrence);
        return operation;
    }
}
