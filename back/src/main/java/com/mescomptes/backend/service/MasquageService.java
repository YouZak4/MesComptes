package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.model.entities.Masquage;
import com.mescomptes.backend.model.enums.TypeElement;
import com.mescomptes.backend.repository.MasquageRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Un utilisateur masque les catégories et libellés fournis dont il ne se sert pas.
 * Les éléments personnels ne se masquent pas : ils s'archivent.
 */
@Service
@RequiredArgsConstructor
public class MasquageService {

    private final MasquageRepository masquageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CategorieService categorieService;
    private final LibelleService libelleService;

    /**
     * Masque un élément fourni. Sans effet s'il l'est déjà.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param type          catégorie ou libellé
     * @param elementId     id de l'élément
     */
    @Transactional
    public void masquer(UUID utilisateurId, TypeElement type, Long elementId) {
        verifierFourni(utilisateurId, type, elementId);
        if (masquageRepository.findByUtilisateurIdAndTypeElementAndElementId(utilisateurId, type, elementId).isEmpty()) {
            Masquage masquage = new Masquage();
            masquage.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
            masquage.setTypeElement(type);
            masquage.setElementId(elementId);
            masquageRepository.save(masquage);
        }
    }

    /**
     * Réaffiche un élément masqué. Sans effet s'il ne l'était pas ; 404 s'il n'existe pas, comme pour le masquage.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param type          catégorie ou libellé
     * @param elementId     id de l'élément
     */
    @Transactional
    public void afficher(UUID utilisateurId, TypeElement type, Long elementId) {
        verifierFourni(utilisateurId, type, elementId);
        masquageRepository.findByUtilisateurIdAndTypeElementAndElementId(utilisateurId, type, elementId)
                .ifPresent(masquageRepository::delete);
    }

    // 404 si l'élément n'existe pas ou n'est pas visible pour l'utilisateur, 422 s'il est personnel
    private void verifierFourni(UUID utilisateurId, TypeElement type, Long elementId) {
        boolean fourni = switch (type) {
            case CATEGORIE -> categorieService.trouverVisible(utilisateurId, elementId).isFournie();
            case LIBELLE -> libelleService.trouverVisible(utilisateurId, elementId).isFourni();
        };
        if (!fourni) throw new BusinessRuleException("Seuls les éléments fournis se masquent ; un élément personnel s'archive.");
    }
}
