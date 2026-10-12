package com.mescomptes.backend.controller;

import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.enums.TypeElement;
import com.mescomptes.backend.service.MasquageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

// /masquages/categorie/{id} ou /masquages/libelle/{id}
@RestController
@RequestMapping("/masquages/{type}/{id}")
@RequiredArgsConstructor
public class MasquageController {

    private final MasquageService masquageService;

    @PutMapping
    public ResponseEntity<Void> masquer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                        @PathVariable String type, @PathVariable Long id) {
        masquageService.masquer(utilisateur.getId(), versType(type), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> afficher(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                         @PathVariable String type, @PathVariable Long id) {
        masquageService.afficher(utilisateur.getId(), versType(type), id);
        return ResponseEntity.noContent().build();
    }

    private static TypeElement versType(String type) {
        try {
            return TypeElement.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Type d'élément inconnu : " + type + " (attendu : categorie ou libelle).");
        }
    }
}
