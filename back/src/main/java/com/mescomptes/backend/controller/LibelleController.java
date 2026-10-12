package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoLibelle;
import com.mescomptes.backend.model.dto.DtoLibelleSaisie;
import com.mescomptes.backend.service.LibelleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/libelles")
@RequiredArgsConstructor
public class LibelleController {

    private final LibelleService libelleService;

    // Sans q : tous les libellés avec leurs indicateurs. Avec q : autocomplétion de la saisie, triée par usage
    @GetMapping
    public List<DtoLibelle> lister(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                   @RequestParam(required = false) String q) {
        return q == null || q.isBlank()
                ? libelleService.lister(utilisateur.getId())
                : libelleService.rechercher(utilisateur.getId(), q);
    }

    @PostMapping
    public ResponseEntity<DtoLibelle> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                            @Valid @RequestBody DtoLibelleSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libelleService.creer(utilisateur.getId(), dto));
    }

    @PutMapping("/{id}")
    public DtoLibelle modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                               @PathVariable Long id, @Valid @RequestBody DtoLibelleSaisie dto) {
        return libelleService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si le libellé est supprimé, 200 avec le libellé s'il a été archivé parce qu'il est utilisé
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoLibelle> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return libelleService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
