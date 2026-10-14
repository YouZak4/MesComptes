package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoCategorie;
import com.mescomptes.backend.model.dto.DtoCategorieSaisie;
import com.mescomptes.backend.service.CategorieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catégories", description = "Catégories fournies (communes, masquables) et personnelles")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    // Fournies + personnelles, avec les indicateurs fournie, masquée et archivée
    @Operation(summary = "Liste les catégories fournies et personnelles", description = "Avec les indicateurs fournie, masquée et archivée.")
    @GetMapping
    public List<DtoCategorie> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return categorieService.lister(utilisateur.getId());
    }

    @Operation(summary = "Crée une catégorie personnelle")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Catégorie créée"),
            @ApiResponse(responseCode = "409", description = "Nom déjà pris")
    })
    @PostMapping
    public ResponseEntity<DtoCategorie> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                              @Valid @RequestBody DtoCategorieSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieService.creer(utilisateur.getId(), dto));
    }

    @Operation(summary = "Modifie une catégorie personnelle", description = "archive à false désarchive. Le type ne change plus une fois la catégorie utilisée.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégorie modifiée"),
            @ApiResponse(responseCode = "409", description = "Nom déjà pris"),
            @ApiResponse(responseCode = "422", description = "Catégorie fournie, ou type d'une catégorie utilisée")
    })
    @PutMapping("/{id}")
    public DtoCategorie modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                 @PathVariable Long id, @Valid @RequestBody DtoCategorieSaisie dto) {
        return categorieService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si la catégorie est supprimée, 200 avec la catégorie si elle a été archivée parce qu'elle est utilisée
    @Operation(summary = "Retire une catégorie personnelle", description = "Supprimée si elle n'a jamais servi, archivée sinon.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégorie archivée (déjà utilisée)"),
            @ApiResponse(responseCode = "204", description = "Catégorie supprimée"),
            @ApiResponse(responseCode = "422", description = "Catégorie fournie")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoCategorie> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return categorieService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
