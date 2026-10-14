package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.AlreadyExist;
import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.InvalidVerificationCodeException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.exception.TooManyRequestsException;
import com.mescomptes.backend.model.dto.DtoChangementEmail;
import com.mescomptes.backend.model.dto.DtoChangementEmailSaisie;
import com.mescomptes.backend.model.dto.DtoCodeSaisie;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.model.entities.ChangementEmail;
import com.mescomptes.backend.model.entities.Utilisateur;
import com.mescomptes.backend.repository.ChangementEmailRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Changement de l'adresse mail de l'utilisateur connecté, en deux temps comme l'inscription :
 * un code est envoyé à la nouvelle adresse, qui ne remplace l'ancienne qu'une fois ce code saisi.
 * Les erreurs répondent 400 ou 422, jamais 401 : la session est valide (le front déconnecte sur 401).
 */
@Service
@RequiredArgsConstructor
public class ChangementEmailService {

    private static final String AUCUN_CHANGEMENT = "Aucun changement d'adresse mail n'est en cours, veuillez recommencer.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ChangementEmailRepository changementEmailRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final UtilisateurService utilisateurService;
    @Value("${app.verification.expiration-minutes}")
    private long expirationMinutes;
    @Value("${app.verification.max-tentatives}")
    private int maxTentatives;
    @Value("${app.verification.delai-renvoi-secondes}")
    private long delaiRenvoiSecondes;

    /**
     * Le changement en cours, pour réafficher la saisie du code après un rafraîchissement.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return le changement en attente, vide s'il n'y en a pas
     */
    @Transactional(readOnly = true)
    public Optional<DtoChangementEmail> enCours(UUID utilisateurId) {
        return changementEmailRepository.findByUtilisateurId(utilisateurId).map(this::versDto);
    }

    /**
     * Demande le changement : vérifie le mot de passe, puis envoie un code à la nouvelle adresse.
     * Une demande remplace la précédente, mais pas avant le délai de renvoi (sinon, envoi de mails à volonté).
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           nouvelle adresse et mot de passe actuel
     * @return le changement en attente
     */
    @Transactional
    public DtoChangementEmail demander(UUID utilisateurId, DtoChangementEmailSaisie dto) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        if (!passwordEncoder.matches(dto.getMotDePasse(), u.getMotDePasse())) {
            throw new BusinessRuleException("Le mot de passe est incorrect.");
        }
        String email = dto.getNouvelEmail().trim().toLowerCase();
        if (email.equals(u.getEmail())) {
            throw new BusinessRuleException("C'est déjà votre adresse mail.");
        }

        ChangementEmail changement = changementEmailRepository.findByUtilisateurId(utilisateurId).orElse(null);
        if (changement != null) {
            verifierDelaiRenvoi(changement);
        } else {
            changement = new ChangementEmail();
            changement.setUtilisateur(u);
        }
        changement.setNouvelEmail(email);
        // Même réponse que l'adresse soit libre ou non : on ne doit pas pouvoir tester qui est inscrit
        changement.setEmailDejaPris(utilisateurRepository.existsByEmail(email));
        genererEtEnvoyerCode(changement);
        return versDto(changement);
    }

    /**
     * Renvoie un nouveau code ; l'ancien devient invalide et le compteur de tentatives repart à zéro.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return le changement en attente
     */
    @Transactional
    public DtoChangementEmail renvoyerCode(UUID utilisateurId) {
        ChangementEmail changement = trouver(utilisateurId);
        verifierDelaiRenvoi(changement);
        genererEtEnvoyerCode(changement);
        return versDto(changement);
    }

    /**
     * Valide le code : la nouvelle adresse remplace l'ancienne, qui est prévenue du changement.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           le code reçu sur la nouvelle adresse
     * @return l'utilisateur, avec sa nouvelle adresse
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class) // sinon la tentative ratée n'est pas comptée
    public DtoUtilisateur verifier(UUID utilisateurId, DtoCodeSaisie dto) {
        ChangementEmail changement = trouver(utilisateurId);

        if (changement.getExpiration().isBefore(LocalDateTime.now())) {
            changementEmailRepository.delete(changement);
            throw new InvalidVerificationCodeException("Le code a expiré, veuillez recommencer le changement d'adresse.");
        }
        if (changement.getTentatives() >= maxTentatives) {
            changementEmailRepository.delete(changement);
            throw new InvalidVerificationCodeException("Trop de tentatives, veuillez recommencer le changement d'adresse.");
        }
        // Adresse déjà prise : aucun code n'a été envoyé, celui saisi est forcément faux
        if (!passwordEncoder.matches(dto.getCode().trim(), changement.getCodeHash())) {
            changement.setTentatives(changement.getTentatives() + 1);
            throw new InvalidVerificationCodeException("Code incorrect.");
        }

        String email = changement.getNouvelEmail();
        // Re-vérif : l'adresse a pu être inscrite entre-temps
        if (utilisateurRepository.existsByEmail(email)) {
            throw new AlreadyExist("Cette adresse mail vient d'être utilisée par un autre compte.");
        }
        Utilisateur u = changement.getUtilisateur();
        String ancienEmail = u.getEmail();
        u.setEmail(email);
        changementEmailRepository.delete(changement);
        try {
            utilisateurRepository.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            // Deux validations simultanées vers la même adresse : la contrainte unique a bloqué la seconde
            throw new AlreadyExist("Cette adresse mail vient d'être utilisée par un autre compte.");
        }
        mailService.envoyerAvisChangementEmail(ancienEmail, email);
        return utilisateurService.versDto(u);
    }

    /**
     * Abandonne le changement en cours ; l'adresse actuelle reste. Sans changement en cours, ne fait rien.
     *
     * @param utilisateurId id de l'utilisateur connecté
     */
    @Transactional
    public void annuler(UUID utilisateurId) {
        changementEmailRepository.findByUtilisateurId(utilisateurId).ifPresent(changementEmailRepository::delete);
    }

    private ChangementEmail trouver(UUID utilisateurId) {
        return changementEmailRepository.findByUtilisateurId(utilisateurId)
                .orElseThrow(() -> new InvalidVerificationCodeException(AUCUN_CHANGEMENT));
    }

    private void verifierDelaiRenvoi(ChangementEmail changement) {
        long secondesRestantes = secondesAvantRenvoi(changement);
        if (secondesRestantes > 0) {
            throw new TooManyRequestsException("Veuillez patienter " + secondesRestantes + " secondes avant de demander un nouveau code.");
        }
    }

    private void genererEtEnvoyerCode(ChangementEmail changement) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000)); // 000000 à 999999
        changement.setCodeHash(passwordEncoder.encode(code));
        changement.setExpiration(LocalDateTime.now().plusMinutes(expirationMinutes));
        changement.setTentatives(0);
        changement.setDernierEnvoi(LocalDateTime.now());
        changementEmailRepository.save(changement);

        // Adresse d'un autre compte : son titulaire est prévenu, le code n'est jamais communiqué
        if (changement.isEmailDejaPris()) {
            mailService.envoyerAlerteEmailDejaUtilise(changement.getNouvelEmail());
        } else {
            mailService.envoyerCodeChangementEmail(changement.getNouvelEmail(), code);
        }
    }

    /**
     * Arrondi à la seconde supérieure : on n'annonce jamais un délai plus court que le délai réel.
     */
    private long secondesAvantRenvoi(ChangementEmail changement) {
        LocalDateTime prochainEnvoiPossible = changement.getDernierEnvoi().plusSeconds(delaiRenvoiSecondes);
        long millisRestantes = Duration.between(LocalDateTime.now(), prochainEnvoiPossible).toMillis();
        return millisRestantes <= 0 ? 0 : (millisRestantes + 999) / 1000;
    }

    private DtoChangementEmail versDto(ChangementEmail changement) {
        return new DtoChangementEmail(changement.getNouvelEmail(), secondesAvantRenvoi(changement));
    }
}
