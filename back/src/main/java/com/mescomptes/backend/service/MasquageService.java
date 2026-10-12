package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.model.entities.Masquage;
import com.mescomptes.backend.model.enums.TypeElement;
import com.mescomptes.backend.repository.MasquageRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void masquer(Long utilisateurId, TypeElement type, Long elementId) {
        boolean fourni = switch (type) {
            case CATEGORIE -> categorieService.trouverVisible(utilisateurId, elementId).isFournie();
            case LIBELLE -> libelleService.trouverVisible(utilisateurId, elementId).isFourni();
        };
        if (!fourni) throw new BusinessRuleException("Seuls les éléments fournis se masquent ; un élément personnel s'archive.");

        if (masquageRepository.findByUtilisateurIdAndTypeElementAndElementId(utilisateurId, type, elementId).isEmpty()) {
            Masquage masquage = new Masquage();
            masquage.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
            masquage.setTypeElement(type);
            masquage.setElementId(elementId);
            masquageRepository.save(masquage);
        }
    }

    /**
     * Réaffiche un élément masqué. Sans effet s'il ne l'était pas.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param type          catégorie ou libellé
     * @param elementId     id de l'élément
     */
    @Transactional
    public void afficher(Long utilisateurId, TypeElement type, Long elementId) {
        masquageRepository.findByUtilisateurIdAndTypeElementAndElementId(utilisateurId, type, elementId)
                .ifPresent(masquageRepository::delete);
    }
}
