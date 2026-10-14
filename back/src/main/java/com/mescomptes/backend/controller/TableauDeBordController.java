package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoTableauDeBord;
import com.mescomptes.backend.service.TableauDeBordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tableau de bord", description = "Synthèse d'une période")
@RestController
@RequestMapping("/tableau-de-bord")
@RequiredArgsConstructor
public class TableauDeBordController {

    private final TableauDeBordService tableauDeBordService;

    // Sans periodeId : période en cours. Sans compteId : tous les comptes
    @Operation(summary = "Construit le tableau de bord d'une période", description = "Sans periodeId : la période en cours. Sans compteId : tous les comptes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tableau de bord de la période"),
            @ApiResponse(responseCode = "422", description = "Aucune période n'est encore définie")
    })
    @GetMapping
    public DtoTableauDeBord construire(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                       @RequestParam(required = false) Long periodeId,
                                       @RequestParam(required = false) Long compteId) {
        return tableauDeBordService.construire(utilisateur.getId(), periodeId, compteId);
    }
}
