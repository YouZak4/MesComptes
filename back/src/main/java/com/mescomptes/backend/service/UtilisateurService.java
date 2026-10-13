package com.mescomptes.backend.service;

import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.model.entities.Role;
import com.mescomptes.backend.model.entities.Utilisateur;
import com.mescomptes.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Retourne l'utilisateur connecté, sans son mot de passe ni son identifiant de connexion.
     *
     * @param id id de l'utilisateur
     * @return l'utilisateur
     */
    @Transactional(readOnly = true)
    public DtoUtilisateur moi(Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
        return versDto(u);
    }

    /**
     * Convertit un utilisateur en DTO, les rôles réduits à leur nom.
     * À appeler dans une transaction : les rôles sont chargés à la demande.
     *
     * @param u l'utilisateur
     * @return le DTO, sans mot de passe ni identifiant de connexion
     */
    public DtoUtilisateur versDto(Utilisateur u) {
        Set<String> roles = u.getRoles().stream().map(Role::getNom).collect(Collectors.toCollection(LinkedHashSet::new));
        return new DtoUtilisateur(u.getId(), u.getPseudo(), u.getEmail(), roles);
    }

    /**
     * Retourne l'utilisateur à qui appartient l'identifiant en paramètre.
     *
     * @param identifiant identifiant de l'utilisateur
     * @return un utilisateur
     */
    public Optional<Utilisateur> findByIdentifiant(String identifiant) {
        return utilisateurRepository.findByIdentifiant(identifiant);
    }

    /**
     * Vérifie si le pseudo en paramètre existe en base de donnée.
     *
     * @param pseudo pseudo de l'utilisateur
     * @return un boolean
     */
    public boolean existsByPseudo(String pseudo) {
        return utilisateurRepository.existsByPseudo(pseudo);
    }

    /**
     * Retourne l'utilisateur à qui appartient le mail en paramètre.
     *
     * @param email l'email de l'utilisateur
     * @return un utilisateur
     */
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    /**
     * Vérifie si le mail en paramètre existe en base de donnée.
     *
     * @param email l'email de l'utilisateur
     * @return un boolean
     */
    public boolean existsByEmail(String email) {
        return utilisateurRepository.existsByEmail(email);
    }

    /**
     * Vérifie si l'identifiant en paramètre existe en base de donnée.
     *
     * @param identifiant l'identifiant de connexion de l'utilisateur
     * @return un boolean
     */
    public boolean existsByIdentifiant(String identifiant) {
        return utilisateurRepository.existsByIdentifiant(identifiant);
    }

    /**
     * Permet de sauvegarder un utilisateur en base de donnée.
     *
     * @param utilisateur l'utilisateur à sauvegarder
     * @return l'utilisateur sauvegardé
     */
    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.saveAndFlush(utilisateur);
    }
}
