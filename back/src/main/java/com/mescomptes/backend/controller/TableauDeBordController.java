package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoTableauDeBord;
import com.mescomptes.backend.service.TableauDeBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tableau-de-bord")
@RequiredArgsConstructor
public class TableauDeBordController {

    private final TableauDeBordService tableauDeBordService;

    // Sans periodeId : période en cours. Sans compteId : tous les comptes
    @GetMapping
    public DtoTableauDeBord construire(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                       @RequestParam(required = false) Long periodeId,
                                       @RequestParam(required = false) Long compteId) {
        return tableauDeBordService.construire(utilisateur.getId(), periodeId, compteId);
    }
}
