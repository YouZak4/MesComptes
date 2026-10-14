package com.mescomptes.backend.controller;

import com.mescomptes.backend.exception.ResourceNotFoundException;
import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.enums.TypeElement;
import com.mescomptes.backend.service.MasquageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

// /masquages/categorie/{id} ou /masquages/libelle/{id}
@Tag(name = "Masquage", description = "Masquer ou réafficher une catégorie ou un libellé fourni")
@RestController
@RequestMapping("/masquages/{type}/{id}")
@RequiredArgsConstructor
public class MasquageController {

    private final MasquageService masquageService;

    @Operation(summary = "Masque un élément fourni", description = "type : categorie ou libelle. Sans effet s'il est déjà masqué.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Élément masqué"),
            @ApiResponse(responseCode = "404", description = "Type ou élément inconnu"),
            @ApiResponse(responseCode = "422", description = "Élément personnel (il s'archive)")
    })
    @PutMapping
    public ResponseEntity<Void> masquer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                        @PathVariable String type, @PathVariable Long id) {
        masquageService.masquer(utilisateur.getId(), versType(type), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Réaffiche un élément fourni", description = "type : categorie ou libelle. Sans effet s'il n'était pas masqué ou n'existe pas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Élément réaffiché, ou rien à faire"),
            @ApiResponse(responseCode = "404", description = "Type inconnu")
    })
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
