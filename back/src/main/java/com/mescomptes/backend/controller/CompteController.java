package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoCompte;
import com.mescomptes.backend.model.dto.DtoCompteSaisie;
import com.mescomptes.backend.service.CompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comptes")
@RequiredArgsConstructor
public class CompteController {

    private final CompteService compteService;

    @GetMapping
    public List<DtoCompte> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return compteService.lister(utilisateur.getId());
    }

    @PostMapping
    public ResponseEntity<DtoCompte> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                           @Valid @RequestBody DtoCompteSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.creer(utilisateur.getId(), dto));
    }

    @PutMapping("/{id}")
    public DtoCompte modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                              @PathVariable Long id, @Valid @RequestBody DtoCompteSaisie dto) {
        return compteService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si le compte est supprimé, 200 avec le compte s'il a été archivé parce qu'il a des opérations
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoCompte> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return compteService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
