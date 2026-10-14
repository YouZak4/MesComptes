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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Utilisateur", description = "Profil de l'utilisateur connecté")
@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final ChangementEmailService changementEmailService;

    // Seul l'utilisateur connecté est exposé, jamais la liste des autres
    @Operation(summary = "Renvoie l'utilisateur connecté")
    @GetMapping("/moi")
    public DtoUtilisateur moi(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return utilisateurService.moi(utilisateur.getId());
    }

    @Operation(summary = "Modifie le nom et le prénom", description = "Prénom vide : retiré.")
    @PutMapping("/moi")
    public DtoUtilisateur modifierProfil(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                         @Valid @RequestBody DtoProfilSaisie dto) {
        return utilisateurService.modifierProfil(utilisateur.getId(), dto);
    }

    @Operation(summary = "Change le mot de passe", description = "Le mot de passe actuel est exigé.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mot de passe changé"),
            @ApiResponse(responseCode = "422", description = "Mot de passe actuel incorrect")
    })
    @PutMapping("/moi/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                  @Valid @RequestBody DtoMotDePasseSaisie dto) {
        utilisateurService.changerMotDePasse(utilisateur.getId(), dto);
        return ResponseEntity.noContent().build();
    }

    // ── Changement d'adresse mail : demande (code envoyé à la nouvelle adresse), puis vérification du code ──

    @Operation(summary = "Renvoie le changement d'adresse en cours")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Changement en attente du code"),
            @ApiResponse(responseCode = "204", description = "Aucun changement en cours")
    })
    @GetMapping("/moi/email")
    public ResponseEntity<DtoChangementEmail> changementEmailEnCours(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return changementEmailService.enCours(utilisateur.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Demande un changement d'adresse mail", description = "Exige le mot de passe. Un code est envoyé à la nouvelle adresse ; une adresse déjà prise reçoit une alerte au lieu du code, avec la même réponse.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Code envoyé"),
            @ApiResponse(responseCode = "422", description = "Mot de passe incorrect, ou adresse identique"),
            @ApiResponse(responseCode = "429", description = "Délai de renvoi pas encore écoulé")
    })
    @PutMapping("/moi/email")
    public ResponseEntity<DtoChangementEmail> demanderChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                                      @Valid @RequestBody DtoChangementEmailSaisie dto) {
        return ResponseEntity.accepted().body(changementEmailService.demander(utilisateur.getId(), dto)); // 202 : pas encore changée
    }

    @Operation(summary = "Valide le code et change l'adresse", description = "L'ancienne adresse est prévenue par mail.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adresse changée"),
            @ApiResponse(responseCode = "400", description = "Code incorrect ou expiré, trop de tentatives, ou aucun changement en cours"),
            @ApiResponse(responseCode = "409", description = "Adresse prise entre-temps")
    })
    @PostMapping("/moi/email/verification")
    public DtoUtilisateur verifierChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                                  @Valid @RequestBody DtoCodeSaisie dto) {
        return changementEmailService.verifier(utilisateur.getId(), dto);
    }

    @Operation(summary = "Renvoie un nouveau code de changement d'adresse")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Nouveau code envoyé"),
            @ApiResponse(responseCode = "400", description = "Aucun changement en cours"),
            @ApiResponse(responseCode = "429", description = "Délai de renvoi pas encore écoulé")
    })
    @PostMapping("/moi/email/renvoi")
    public ResponseEntity<DtoChangementEmail> renvoyerCodeChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return ResponseEntity.accepted().body(changementEmailService.renvoyerCode(utilisateur.getId()));
    }

    @Operation(summary = "Annule le changement d'adresse en cours", description = "Sans changement en cours, ne fait rien.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Changement annulé")
    })
    @DeleteMapping("/moi/email")
    public ResponseEntity<Void> annulerChangementEmail(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        changementEmailService.annuler(utilisateur.getId());
        return ResponseEntity.noContent().build();
    }
}
