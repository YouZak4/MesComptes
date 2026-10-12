package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoCategorie;
import com.mescomptes.backend.model.dto.DtoCategorieSaisie;
import com.mescomptes.backend.service.CategorieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    // Fournies + personnelles, avec les indicateurs fournie, masquée et archivée
    @GetMapping
    public List<DtoCategorie> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return categorieService.lister(utilisateur.getId());
    }

    @PostMapping
    public ResponseEntity<DtoCategorie> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                              @Valid @RequestBody DtoCategorieSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieService.creer(utilisateur.getId(), dto));
    }

    @PutMapping("/{id}")
    public DtoCategorie modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                 @PathVariable Long id, @Valid @RequestBody DtoCategorieSaisie dto) {
        return categorieService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si la catégorie est supprimée, 200 avec la catégorie si elle a été archivée parce qu'elle est utilisée
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoCategorie> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return categorieService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
