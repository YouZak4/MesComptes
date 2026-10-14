package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoPeriode;
import com.mescomptes.backend.model.dto.DtoPeriodeSaisie;
import com.mescomptes.backend.service.PeriodeService;
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

@Tag(name = "Périodes", description = "Mois de compte aux dates réglables, contigus")
@RestController
@RequestMapping("/periodes")
@RequiredArgsConstructor
public class PeriodeController {

    private final PeriodeService periodeService;

    @Operation(summary = "Liste les périodes")
    @GetMapping
    public List<DtoPeriode> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return periodeService.lister(utilisateur.getId());
    }

    @Operation(summary = "Crée une période", description = "Corps vide : la période suivante, d'un mois. Première période : la date de début est obligatoire.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Période créée"),
            @ApiResponse(responseCode = "422", description = "Dates incohérentes ou trou avec la période précédente")
    })
    @PostMapping
    public ResponseEntity<DtoPeriode> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                            @Valid @RequestBody DtoPeriodeSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodeService.creer(utilisateur.getId(), dto));
    }

    // Déplace aussi la limite de la période voisine
    @Operation(summary = "Modifie une période", description = "Déplacer une date déplace aussi la limite de la période voisine.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Période modifiée (et sa voisine si une limite a bougé)"),
            @ApiResponse(responseCode = "422", description = "Dates incohérentes, ou une voisine perdrait tous ses jours")
    })
    @PutMapping("/{id}")
    public DtoPeriode modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                               @PathVariable Long id, @Valid @RequestBody DtoPeriodeSaisie dto) {
        return periodeService.modifier(utilisateur.getId(), id, dto);
    }
}
