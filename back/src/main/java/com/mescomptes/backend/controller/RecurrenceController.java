package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoRecurrence;
import com.mescomptes.backend.model.dto.DtoRecurrenceSaisie;
import com.mescomptes.backend.service.RecurrenceService;
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

@Tag(name = "Récurrences", description = "Modèles d'opérations ajoutées d'elles-mêmes chaque mois ou chaque année")
@RestController
@RequestMapping("/recurrences")
@RequiredArgsConstructor
public class RecurrenceController {

    private final RecurrenceService recurrenceService;

    @Operation(summary = "Liste les récurrences, actives et arrêtées")
    @GetMapping
    public List<DtoRecurrence> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return recurrenceService.lister(utilisateur.getId());
    }

    @Operation(summary = "Crée une récurrence", description = "Les occurrences passées depuis la date de début sont générées tout de suite.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Récurrence créée"),
            @ApiResponse(responseCode = "422", description = "Dates incohérentes ou élément archivé")
    })
    @PostMapping
    public ResponseEntity<DtoRecurrence> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                               @Valid @RequestBody DtoRecurrenceSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurrenceService.creer(utilisateur.getId(), dto));
    }

    // Ne touche que les occurrences futures
    @Operation(summary = "Modifie une récurrence", description = "Ne touche que les occurrences futures ; les opérations déjà générées restent.")
    @PutMapping("/{id}")
    public DtoRecurrence modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                  @PathVariable Long id, @Valid @RequestBody DtoRecurrenceSaisie dto) {
        return recurrenceService.modifier(utilisateur.getId(), id, dto);
    }

    // 200 avec la récurrence arrêtée (historique conservé), 204 si elle n'avait rien généré et a été supprimée
    @Operation(summary = "Arrête une récurrence", description = "Arrêtée à aujourd'hui si elle a généré des opérations (historique conservé), supprimée sinon.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Récurrence arrêtée"),
            @ApiResponse(responseCode = "204", description = "Récurrence supprimée (rien n'avait été généré)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoRecurrence> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return recurrenceService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
