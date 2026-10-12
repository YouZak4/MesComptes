package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.AlreadyExist;
import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.dto.DtoCategorie;
import com.mescomptes.backend.model.dto.DtoCategorieSaisie;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.enums.TypeElement;
import com.mescomptes.backend.repository.CategorieRepository;
import com.mescomptes.backend.repository.LibelleRepository;
import com.mescomptes.backend.repository.MasquageRepository;
import com.mescomptes.backend.repository.OperationRepository;
import com.mescomptes.backend.repository.RecurrenceRepository;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private static final String CATEGORIE_INTROUVABLE = "Cette catégorie n'existe pas.";
    private static final String CATEGORIE_FOURNIE = "Les catégories fournies ne sont pas modifiables, mais vous pouvez les masquer.";
    private static final String TYPE_NON_MODIFIABLE = "Cette catégorie est déjà utilisée : son type (dépense ou revenu) ne peut plus changer.";

    private final CategorieRepository categorieRepository;
    private final LibelleRepository libelleRepository;
    private final MasquageRepository masquageRepository;
    private final OperationRepository operationRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Retourne les catégories fournies et personnelles, masquées et archivées comprises, avec leurs indicateurs.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return les catégories triées par type puis par nom
     */
    @Transactional(readOnly = true)
    public List<DtoCategorie> lister(Long utilisateurId) {
        Set<Long> masquees = masquageRepository.findIdsMasques(utilisateurId, TypeElement.CATEGORIE);
        return categorieRepository.findVisibles(utilisateurId).stream()
                .map(categorie -> versDto(categorie, masquees.contains(categorie.getId())))
                .toList();
    }

    /**
     * Crée une catégorie personnelle.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           nom, type, icône et couleur
     * @return la catégorie créée
     */
    @Transactional
    public DtoCategorie creer(Long utilisateurId, DtoCategorieSaisie dto) {
        verifierNomLibre(utilisateurId, dto.getNom(), null);
        Categorie categorie = new Categorie();
        categorie.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        appliquer(categorie, dto);
        categorie.setArchive(false);
        return versDto(enregistrer(categorie), false);
    }

    /**
     * Modifie une catégorie personnelle ; permet aussi de l'archiver ou de la désarchiver.
     * Le type ne peut plus changer une fois la catégorie utilisée, pour rester cohérent avec les opérations.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de la catégorie
     * @param dto           nouvelles valeurs
     * @return la catégorie modifiée
     */
    @Transactional
    public DtoCategorie modifier(Long utilisateurId, Long id, DtoCategorieSaisie dto) {
        Categorie categorie = trouverPersonnelle(utilisateurId, id);
        verifierNomLibre(utilisateurId, dto.getNom(), id);
        if (dto.getType() != categorie.getType()
                && (operationRepository.existsByCategorieId(id) || recurrenceRepository.existsByCategorieId(id))) {
            throw new BusinessRuleException(TYPE_NON_MODIFIABLE);
        }
        appliquer(categorie, dto);
        categorie.setArchive(dto.isArchive());
        return versDto(enregistrer(categorie), false);
    }

    /**
     * Supprime une catégorie personnelle inutilisée ; une catégorie déjà utilisée (opération, récurrence
     * ou catégorie par défaut d'un libellé) est archivée pour que l'historique la garde.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id de la catégorie
     * @return la catégorie archivée, ou vide si elle a été supprimée
     */
    @Transactional
    public Optional<DtoCategorie> supprimer(Long utilisateurId, Long id) {
        Categorie categorie = trouverPersonnelle(utilisateurId, id);
        if (operationRepository.existsByCategorieId(id) || recurrenceRepository.existsByCategorieId(id)
                || libelleRepository.existsByCategorieParDefautId(id)) {
            categorie.setArchive(true);
            return Optional.of(versDto(categorie, false));
        }
        categorieRepository.delete(categorie);
        return Optional.empty();
    }

    /**
     * Retourne une catégorie fournie ou appartenant à l'utilisateur, ou une 404.
     */
    @Transactional(readOnly = true)
    public Categorie trouverVisible(Long utilisateurId, Long id) {
        return categorieRepository.findVisible(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORIE_INTROUVABLE));
    }

    private Categorie trouverPersonnelle(Long utilisateurId, Long id) {
        Categorie categorie = trouverVisible(utilisateurId, id);
        if (categorie.isFournie()) throw new BusinessRuleException(CATEGORIE_FOURNIE);
        return categorie;
    }

    // Le nom ne doit exister ni parmi les catégories fournies ni parmi celles de l'utilisateur (hors elle-même)
    private void verifierNomLibre(Long utilisateurId, String nom, Long idModifie) {
        boolean pris = categorieRepository.findVisiblesParNom(Noms.normaliser(nom), utilisateurId).stream()
                .anyMatch(existante -> !existante.getId().equals(idModifie));
        if (pris) throw new AlreadyExist(nomDejaPris(nom));
    }

    private Categorie enregistrer(Categorie categorie) {
        try {
            return categorieRepository.saveAndFlush(categorie);
        } catch (DataIntegrityViolationException e) {
            // Deux créations simultanées du même nom : la contrainte unique en base a bloqué la seconde
            throw new AlreadyExist(nomDejaPris(categorie.getNom()));
        }
    }

    private static String nomDejaPris(String nom) {
        return "La catégorie « " + Noms.nettoyer(nom) + " » existe déjà.";
    }

    private void appliquer(Categorie categorie, DtoCategorieSaisie dto) {
        categorie.setNom(Noms.nettoyer(dto.getNom()));
        categorie.setType(dto.getType());
        categorie.setIcone(dto.getIcone());
        categorie.setCouleur(dto.getCouleur());
    }

    private DtoCategorie versDto(Categorie categorie, boolean masquee) {
        return new DtoCategorie(categorie.getId(), categorie.getNom(), categorie.getType(), categorie.getIcone(),
                categorie.getCouleur(), categorie.isFournie(), masquee, categorie.isArchive());
    }
}
