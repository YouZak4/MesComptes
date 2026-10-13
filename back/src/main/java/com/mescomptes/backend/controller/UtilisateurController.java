package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoMotDePasseSaisie;
import com.mescomptes.backend.model.dto.DtoProfilSaisie;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

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
}
