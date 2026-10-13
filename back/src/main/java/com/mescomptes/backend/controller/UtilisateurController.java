package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
