package com.mescomptes.backend.service;

import com.mescomptes.backend.model.entities.Recurrence;
import com.mescomptes.backend.repository.RecurrenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Déclenche la génération des récurrences : chaque nuit, au démarrage du serveur et à la connexion
 * d'un utilisateur (rattrapage des jours où le serveur était arrêté).
 * Volontairement hors transaction : chaque récurrence est générée dans la sienne, une erreur n'arrête pas les autres.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificationRecurrences {

    private final RecurrenceRepository recurrenceRepository;
    private final GenerateurRecurrences generateur;

    @Scheduled(cron = "${app.recurrences.generation-cron}")
    @EventListener(ApplicationReadyEvent.class)
    public void genererEcheances() {
        int creees = generer(recurrenceRepository.findAGenerer(LocalDate.now()));
        if (creees > 0) log.info("{} opération(s) générée(s) par les récurrences.", creees);
    }

    /**
     * Rattrapage des récurrences d'un utilisateur, appelé à sa connexion.
     *
     * @param utilisateurId id de l'utilisateur qui se connecte
     */
    public void rattraper(Long utilisateurId) {
        generer(recurrenceRepository.findAGenererPour(utilisateurId, LocalDate.now()));
    }

    private int generer(List<Recurrence> recurrences) {
        int creees = 0;
        for (Recurrence recurrence : recurrences) {
            try {
                creees += generateur.generer(recurrence.getId(), LocalDate.now());
            } catch (RuntimeException e) {
                // Par exemple deux générations simultanées de la même occurrence (unicité récurrence + date)
                log.warn("Génération de la récurrence {} impossible : {}", recurrence.getId(), e.getMessage());
            }
        }
        return creees;
    }
}
