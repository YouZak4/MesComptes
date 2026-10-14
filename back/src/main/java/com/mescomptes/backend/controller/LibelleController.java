package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoLibelle;
import com.mescomptes.backend.model.dto.DtoLibelleSaisie;
import com.mescomptes.backend.service.LibelleService;
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

@Tag(name = "Libellés", description = "Libellés fournis et personnels, chacun avec une catégorie par défaut")
@RestController
@RequestMapping("/libelles")
@RequiredArgsConstructor
public class LibelleController {

    private final LibelleService libelleService;

    // Sans q : tous les libellés avec leurs indicateurs. Avec q : autocomplétion de la saisie, triée par usage
    @Operation(summary = "Liste les libellés, ou les cherche pour l'autocomplétion", description = "Sans q : tous les libellés avec leurs indicateurs. Avec q : libellés visibles dont le nom contient q, les plus utilisés d'abord.")
    @GetMapping
    public List<DtoLibelle> lister(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                   @RequestParam(required = false) String q) {
        return q == null || q.isBlank()
                ? libelleService.lister(utilisateur.getId())
                : libelleService.rechercher(utilisateur.getId(), q);
    }

    @Operation(summary = "Crée un libellé personnel")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libellé créé"),
            @ApiResponse(responseCode = "409", description = "Nom déjà pris"),
            @ApiResponse(responseCode = "422", description = "Catégorie archivée")
    })
    @PostMapping
    public ResponseEntity<DtoLibelle> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                            @Valid @RequestBody DtoLibelleSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libelleService.creer(utilisateur.getId(), dto));
    }

    @Operation(summary = "Modifie un libellé personnel", description = "archive à false désarchive.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libellé modifié"),
            @ApiResponse(responseCode = "409", description = "Nom déjà pris"),
            @ApiResponse(responseCode = "422", description = "Libellé fourni, ou nouvelle catégorie archivée")
    })
    @PutMapping("/{id}")
    public DtoLibelle modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                               @PathVariable Long id, @Valid @RequestBody DtoLibelleSaisie dto) {
        return libelleService.modifier(utilisateur.getId(), id, dto);
    }

    // 204 si le libellé est supprimé, 200 avec le libellé s'il a été archivé parce qu'il est utilisé
    @Operation(summary = "Retire un libellé personnel", description = "Supprimé s'il n'a jamais servi, archivé sinon.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libellé archivé (déjà utilisé)"),
            @ApiResponse(responseCode = "204", description = "Libellé supprimé"),
            @ApiResponse(responseCode = "422", description = "Libellé fourni")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoLibelle> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return libelleService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
