package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.AlreadyExist;
import com.mescomptes.backend.exception.BusinessRuleException;
import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.Noms;
import com.mescomptes.backend.model.dto.DtoLibelle;
import com.mescomptes.backend.model.dto.DtoLibelleSaisie;
import com.mescomptes.backend.model.entities.Categorie;
import com.mescomptes.backend.model.entities.Libelle;
import com.mescomptes.backend.model.enums.TypeElement;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibelleService {

    private static final int NB_SUGGESTIONS = 20;
    private static final String LIBELLE_INTROUVABLE = "Ce libellé n'existe pas.";
    private static final String LIBELLE_FOURNI = "Les libellés fournis ne sont pas modifiables, mais vous pouvez les masquer.";
    private static final String CATEGORIE_ARCHIVEE = "Cette catégorie est archivée : choisissez-en une autre ou désarchivez-la.";

    private final LibelleRepository libelleRepository;
    private final MasquageRepository masquageRepository;
    private final OperationRepository operationRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CategorieService categorieService;

    /**
     * Retourne les libellés fournis et personnels, masqués et archivés compris, avec leurs indicateurs.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @return les libellés triés par nom
     */
    @Transactional(readOnly = true)
    public List<DtoLibelle> lister(UUID utilisateurId) {
        Set<Long> masques = masquageRepository.findIdsMasques(utilisateurId, TypeElement.LIBELLE);
        return libelleRepository.findVisibles(utilisateurId).stream()
                .map(libelle -> versDto(libelle, masques.contains(libelle.getId())))
                .toList();
    }

    /**
     * Autocomplétion de la saisie : libellés contenant le texte, les plus utilisés d'abord.
     * Sont exclus les libellés archivés ou masqués, et ceux dont la catégorie est archivée ou masquée.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param texte         texte tapé (casse et espaces en trop ignorés)
     * @return au plus 20 libellés
     */
    @Transactional(readOnly = true)
    public List<DtoLibelle> rechercher(UUID utilisateurId, String texte) {
        Set<Long> libellesMasques = masquageRepository.findIdsMasques(utilisateurId, TypeElement.LIBELLE);
        Set<Long> categoriesMasquees = masquageRepository.findIdsMasques(utilisateurId, TypeElement.CATEGORIE);
        return libelleRepository.rechercher(utilisateurId, Noms.motifRecherche(texte)).stream()
                .filter(libelle -> !libellesMasques.contains(libelle.getId()))
                .filter(libelle -> !categoriesMasquees.contains(libelle.getCategorieParDefaut().getId()))
                .filter(libelle -> !libelle.getCategorieParDefaut().isArchive())
                .limit(NB_SUGGESTIONS)
                .map(libelle -> versDto(libelle, false))
                .toList();
    }

    /**
     * Crée un libellé personnel (depuis les réglages ou directement depuis la saisie).
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param dto           nom et catégorie par défaut
     * @return le libellé créé
     */
    @Transactional
    public DtoLibelle creer(UUID utilisateurId, DtoLibelleSaisie dto) {
        verifierNomLibre(utilisateurId, dto.getNom(), null);
        Libelle libelle = new Libelle();
        libelle.setUtilisateur(utilisateurRepository.getReferenceById(utilisateurId));
        appliquer(utilisateurId, libelle, dto);
        libelle.setArchive(false);
        return versDto(enregistrer(libelle), false);
    }

    /**
     * Modifie un libellé personnel ; permet aussi de l'archiver ou de le désarchiver.
     * Les opérations existantes gardent leur catégorie.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id du libellé
     * @param dto           nouvelles valeurs
     * @return le libellé modifié
     */
    @Transactional
    public DtoLibelle modifier(UUID utilisateurId, Long id, DtoLibelleSaisie dto) {
        Libelle libelle = trouverPersonnel(utilisateurId, id);
        verifierNomLibre(utilisateurId, dto.getNom(), id);
        appliquer(utilisateurId, libelle, dto);
        if (dto.getArchive() != null) libelle.setArchive(dto.getArchive());
        return versDto(enregistrer(libelle), false);
    }

    /**
     * Supprime un libellé personnel inutilisé ; un libellé déjà utilisé est archivé pour que l'historique le garde.
     *
     * @param utilisateurId id de l'utilisateur connecté
     * @param id            id du libellé
     * @return le libellé archivé, ou vide s'il a été supprimé
     */
    @Transactional
    public Optional<DtoLibelle> supprimer(UUID utilisateurId, Long id) {
        Libelle libelle = trouverPersonnel(utilisateurId, id);
        if (operationRepository.existsByLibelleId(id) || recurrenceRepository.existsByLibelleId(id)) {
            libelle.setArchive(true);
            return Optional.of(versDto(libelle, false));
        }
        libelleRepository.delete(libelle);
        return Optional.empty();
    }

    /**
     * Retourne un libellé fourni ou appartenant à l'utilisateur, ou une 404.
     */
    @Transactional(readOnly = true)
    public Libelle trouverVisible(UUID utilisateurId, Long id) {
        return libelleRepository.findVisible(id, utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException(LIBELLE_INTROUVABLE));
    }

    private Libelle trouverPersonnel(UUID utilisateurId, Long id) {
        Libelle libelle = trouverVisible(utilisateurId, id);
        if (libelle.isFourni()) throw new BusinessRuleException(LIBELLE_FOURNI);
        return libelle;
    }

    // Le nom ne doit exister ni parmi les libellés fournis ni parmi ceux de l'utilisateur (hors lui-même)
    private void verifierNomLibre(UUID utilisateurId, String nom, Long idModifie) {
        boolean pris = libelleRepository.findVisiblesParNom(Noms.normaliser(nom), utilisateurId).stream()
                .anyMatch(existant -> !existant.getId().equals(idModifie));
        if (pris) throw new AlreadyExist(nomDejaPris(nom));
    }

    private Libelle enregistrer(Libelle libelle) {
        try {
            return libelleRepository.saveAndFlush(libelle);
        } catch (DataIntegrityViolationException e) {
            // Deux créations simultanées du même nom : la contrainte unique en base a bloqué la seconde
            throw new AlreadyExist(nomDejaPris(libelle.getNom()));
        }
    }

    private static String nomDejaPris(String nom) {
        return "Le libellé « " + Noms.nettoyer(nom) + " » existe déjà.";
    }

    private void appliquer(UUID utilisateurId, Libelle libelle, DtoLibelleSaisie dto) {
        Categorie categorie = categorieService.trouverVisible(utilisateurId, dto.getCategorieId());
        // On laisse un libellé existant sur sa catégorie archivée, mais on n'en choisit pas une nouvellement
        boolean changeDeCategorie = libelle.getCategorieParDefaut() == null || !libelle.getCategorieParDefaut().getId().equals(categorie.getId());
        if (categorie.isArchive() && changeDeCategorie) throw new BusinessRuleException(CATEGORIE_ARCHIVEE);
        libelle.setNom(Noms.nettoyer(dto.getNom()));
        libelle.setCategorieParDefaut(categorie);
    }

    private DtoLibelle versDto(Libelle libelle, boolean masque) {
        return new DtoLibelle(libelle.getId(), libelle.getNom(), libelle.getCategorieParDefaut().getId(),
                libelle.isFourni(), masque, libelle.isArchive());
    }
}
