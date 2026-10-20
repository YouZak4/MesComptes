package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoOperation;
import com.mescomptes.backend.model.dto.DtoOperationSaisie;
import com.mescomptes.backend.model.dto.DtoRecurrence;
import com.mescomptes.backend.model.dto.FiltreOperations;
import com.mescomptes.backend.model.enums.Frequence;
import com.mescomptes.backend.service.OperationService;
import com.mescomptes.backend.service.RecurrenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Opérations", description = "Dépenses et revenus ; le type est déduit de la catégorie")
@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final RecurrenceService recurrenceService;

    // Filtres facultatifs : ?periodeId=&compteId=&categorieId=&libelleId=&q=
    @Operation(summary = "Liste les opérations, filtrées", description = "Filtres facultatifs et combinables. q cherche dans la note et le libellé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opérations, des plus récentes aux plus anciennes"),
            @ApiResponse(responseCode = "404", description = "Un filtre désigne une période, un compte, une catégorie ou un libellé introuvable")
    })
    @GetMapping
    public List<DtoOperation> lister(@AuthenticationPrincipal CustomUserDetails utilisateur, @ParameterObject FiltreOperations filtre) {
        return operationService.lister(utilisateur.getId(), filtre);
    }

    @Operation(summary = "Crée une opération", description = "Date vide : aujourd'hui. Catégorie vide : celle du libellé. Crée les périodes manquantes pour couvrir sa date.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Opération créée"),
            @ApiResponse(responseCode = "422", description = "Compte, catégorie ou libellé archivé, ou catégorie manquante")
    })
    @PostMapping
    public ResponseEntity<DtoOperation> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                              @Valid @RequestBody DtoOperationSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operationService.creer(utilisateur.getId(), dto));
    }

    @Operation(summary = "Modifie une opération", description = "Une opération générée par une récurrence ne change qu'elle-même.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opération modifiée"),
            @ApiResponse(responseCode = "422", description = "Élément archivé, ou la récurrence a déjà une opération à cette date")
    })
    @PutMapping("/{id}")
    public DtoOperation modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                 @PathVariable Long id, @Valid @RequestBody DtoOperationSaisie dto) {
        return operationService.modifier(utilisateur.getId(), id, dto);
    }

    // « Répéter chaque mois » : l'opération devient la première occurrence d'une nouvelle récurrence
    @Operation(summary = "Répète une opération (« Répéter chaque mois »)", description = "L'opération devient la première occurrence d'une nouvelle récurrence.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Récurrence créée"),
            @ApiResponse(responseCode = "422", description = "L'opération fait déjà partie d'une récurrence")
    })
    @PostMapping("/{id}/repeter")
    public ResponseEntity<DtoRecurrence> repeter(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id,
                                                 @RequestParam(defaultValue = "MENSUELLE") Frequence frequence) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurrenceService.repeter(utilisateur.getId(), id, frequence));
    }

    @Operation(summary = "Supprime une opération", description = "Une opération générée par une récurrence n'est pas recréée.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opération supprimée")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        operationService.supprimer(utilisateur.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
