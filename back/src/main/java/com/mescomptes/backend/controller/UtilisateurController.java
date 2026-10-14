package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoChangementEmail;
import com.mescomptes.backend.model.dto.DtoChangementEmailSaisie;
import com.mescomptes.backend.model.dto.DtoCodeSaisie;
import com.mescomptes.backend.model.dto.DtoMotDePasseSaisie;
import com.mescomptes.backend.model.dto.DtoProfilSaisie;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.service.ChangementEmailService;
import com.mescomptes.backend.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final ChangementEmailService changementEmailService;

    // Seul l'utilisateur connecté est exposé, jamais la liste des autres
    @GetMapping("/moi")
    public DtoUtilisateur moi(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return utilisateurService.moi(utilisateur.getId());
    }

    @PutMapping("/moi")
    public DtoUtilisateur modifierProfil(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                         @Valid @RequestBody DtoProfilSaisie dto) {
        return utilisateurService.modifierProfil(utilisateur.getId(), dto);
    }

    @PutMapping("/moi/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                  @Valid @RequestBody DtoMotDePasseSaisie dto) {
        utilisateurService.changerMotDePasse(utilisateur.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    // ── Changement d'adresse mail : demande (code envoyé à la nouvelle adresse), puis vérification du code ──

    @GetMapping("/moi/email")
    public ResponseEntity<DtoChangementEmail> changementEmailEnCours(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return changementEmailService.enCours(utilisateur.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/moi/email")
    public ResponseEntity<DtoChangementEmail> demanderChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                                      @Valid @RequestBody DtoChangementEmailSaisie dto) {
        return ResponseEntity.accepted().body(changementEmailService.demander(utilisateur.getId(), dto)); // 202 : pas encore changée
    }

    @PostMapping("/moi/email/verification")
    public DtoUtilisateur verifierChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                  @Valid @RequestBody DtoCodeSaisie dto) {
        return changementEmailService.verifier(utilisateur.getId(), dto);
    }

    @PostMapping("/moi/email/renvoi")
    public ResponseEntity<DtoChangementEmail> renvoyerCodeChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.accepted().body(changementEmailService.renvoyerCode(utilisateur.getId()));
    }

    @DeleteMapping("/moi/email")
    public ResponseEntity<Void> annulerChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        changementEmailService.annuler(utilisateur.getId());
        return ResponseEntity.noContent().build();
    }
}
