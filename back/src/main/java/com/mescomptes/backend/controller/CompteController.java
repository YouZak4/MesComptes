package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoCompte;
import com.mescomptes.backend.model.dto.DtoCompteSaisie;
import com.mescomptes.backend.service.CompteService;
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

@Tag(name = "Comptes", description = "Comptes de l'utilisateur et leur solde")
@RestController
@RequestMapping("/comptes")
@RequiredArgsConstructor
public class CompteController {

    private final CompteService compteService;

    @Operation(summary = "Liste les comptes, archivés compris, avec leur solde")
    @GetMapping
    public List<DtoCompte> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return compteService.lister(utilisateur.getId());
    }

    @Operation(summary = "Crée un compte")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé")
    })
    @PostMapping
    public ResponseEntity<DtoCompte> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                           @Valid @RequestBody DtoCompteSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.creer(utilisateur.getId(), dto));
    }

    @Operation(summary = "Modifie un compte", description = "archive à false désarchive le compte.")
    @PutMapping("/{id}")
    public DtoCompte modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                              @PathVariable Long id, @Valid @RequestBody DtoCompteSaisie dto) {
        return compteService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si le compte est supprimé, 200 avec le compte s'il a été archivé parce qu'il a des opérations
    @Operation(summary = "Retire un compte", description = "Supprimé s'il n'a aucune opération, archivé sinon.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte archivé (il a des opérations)"),
            @ApiResponse(responseCode = "204", description = "Compte supprimé")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoCompte> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return compteService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
