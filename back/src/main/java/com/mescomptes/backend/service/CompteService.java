package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.dto.DtoCompte;
import com.mescomptes.backend.model.dto.DtoCompteSaisie;
import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.entities.Compte;
import com.mescomptes.backend.model.entities.Utilisateur;
import com.mescomptes.backend.model.enums.TypeOperation;
import com.mescomptes.backend.repository.CompteRepository;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompteService {

    private static final String COMPTE_PAR_DEFAUT = "Compte courant";
    private static final String COMPTE_INTROUVABLE = "Ce compte n'existe pas.";

    private final CompteRepository compteRepository;
    private final OperationRepository operationRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Retourne tous les comptes de l'utilisateur, archivés compris, avec leur solde.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return la liste des comptes triée par nom
     */
    @Transactional(readOnly = true)
    public List<DtoCompte> lister(Long utilisateurId) {
        Map<Long, BigDecimal> mouvements = operationRepository.sommeMouvementsParCompte(utilisateurId, TypeOperation.REVENU).stream()
                .collect(Collectors.toMap(ligne -> (Long) ligne[0], ligne -> (BigDecimal) ligne[1]));
        return compteRepository.findByUtilisateurIdOrderByNom(utilisateurId).stream()
                .map(compte -> versDto(compte, mouvements.getOrDefault(compte.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Crée un compte pour l'utilisateur connecté.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           nom, solde initial et date du solde initial
     * @return le compte créé
     */
    @Transactional
    public DtoCompte creer(Long utilisateurId, DtoCompteSaisie dto) {
        Compte compte = new Compte();
        compte.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        appliquer(compte, dto);
        compte.setArchive(false);
        return versDto(compteRepository.save(compte), BigDecimal.ZERO);
    }

    /**
     * Modifie un compte ; permet aussi de l'archiver ou de le désarchiver.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id du compte
     * @param dto           nouvelles valeurs
     * @return le compte modifié, avec son solde recalculé
     */
    @Transactional
    public DtoCompte modifier(Long utilisateurId, Long id, DtoCompteSaisie dto) {
        Compte compte = trouver(utilisateurId, id);
        appliquer(compte, dto);
        compte.setArchive(dto.isArchive());
        return relire(utilisateurId, id);
    }

    /**
     * Supprime un compte sans opération ; un compte qui en a est archivé pour garder son historique.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id du compte
     * @return le compte archivé, ou vide s'il a été supprimé
     */
    @Transactional
    public Optional<DtoCompte> supprimer(Long utilisateurId, Long id) {
        Compte compte = trouver(utilisateurId, id);
        if (operationRepository.existsByCompteId(id)) {
            compte.setArchive(true);
            return Optional.of(relire(utilisateurId, id));
        }
        compteRepository.delete(compte);
        return Optional.empty();
    }

    /**
     * Crée le premier compte d'un utilisateur à son inscription, pour qu'il puisse saisir tout de suite.
     *
     * @param utilisateur l'utilisateur qui vient d'être créé
     */
    @Transactional
    public void creerCompteParDefaut(Utilisateur utilisateur) {
        Compte compte = new Compte();
        compte.setUtilisateur(utilisateur);
        compte.setNom(COMPTE_PAR_DEFAUT);
        compte.setSoldeInitial(BigDecimal.ZERO);
        compte.setDateSoldeInitial(LocalDate.now());
        compteRepository.save(compte);
    }

    /**
     * Retourne un compte de l'utilisateur, ou une 404 s'il n'existe pas ou appartient à un autre utilisateur.
     */
    @Transactional(readOnly = true)
    public Compte trouver(Long utilisateurId, Long id) {
        return compteRepository.findByIdAndUtilisateurId(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPTE_INTROUVABLE));
    }

    // Relit le compte avec son solde, la date du solde initial ayant pu changer
    private DtoCompte relire(Long utilisateurId, Long id) {
        compteRepository.flush();
        return lister(utilisateurId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private void appliquer(Compte compte, DtoCompteSaisie dto) {
        compte.setNom(Noms.nettoyer(dto.getNom()));
        compte.setSoldeInitial(dto.getSoldeInitial());
        compte.setDateSoldeInitial(dto.getDateSoldeInitial());
    }

    private DtoCompte versDto(Compte compte, BigDecimal mouvements) {
        return new DtoCompte(compte.getId(), compte.getNom(), compte.getSoldeInitial(), compte.getDateSoldeInitial(),
                compte.isArchive(), compte.getSoldeInitial().add(mouvements));
    }
}
