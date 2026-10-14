package com.mescomptes.backend.service;

import com.mescomptes.backend.repository.ChangementEmailRepository;
import com.mescomptes.backend.repository.InscriptionEnAttenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Supprime les inscriptions et les changements d'adresse mail jamais finalisés : sans ce nettoyage, les données
 * (e-mail, nom, identifiant, mot de passe haché) resteraient en base indéfiniment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NettoyageInscriptionService {

    private final InscriptionEnAttenteRepository inscriptionEnAttenteRepository;
    private final ChangementEmailRepository changementEmailRepository;
    @Value("${app.verification.duree-vie-heures}")
    private long dureeVieHeures;

    @Scheduled(cron = "${app.verification.nettoyage-cron}")
    @Transactional
    public void supprimerInscriptionsAbandonnees() {
        LocalDateTime limite = LocalDateTime.now().minusHours(dureeVieHeures);
        int supprimees = inscriptionEnAttenteRepository.deleteByDernierEnvoiBefore(limite);
        if (supprimees > 0) {
            log.info("{} inscription(s) en attente abandonnée(s) supprimée(s).", supprimees);
        }
        int changements = changementEmailRepository.deleteByDernierEnvoiBefore(limite);
        if (changements > 0) {
            log.info("{} changement(s) d'adresse mail abandonné(s) supprimé(s).", changements);
        }
    }
}
