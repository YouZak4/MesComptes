package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoPeriode;
import com.mescomptes.backend.model.dto.DtoPeriodeSaisie;
import com.mescomptes.backend.service.PeriodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/periodes")
@RequiredArgsConstructor
public class PeriodeController {

    private final PeriodeService periodeService;

    @GetMapping
    public List<DtoPeriode> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return periodeService.lister(utilisateur.getId());
    }

    @PostMapping
    public ResponseEntity<DtoPeriode> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                            @Valid @RequestBody DtoPeriodeSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodeService.creer(utilisateur.getId(), dto));
    }

    // Déplace aussi la limite de la période voisine
    @PutMapping("/{id}")
    public DtoPeriode modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                               @PathVariable Long id, @Valid @RequestBody DtoPeriodeSaisie dto) {
        return periodeService.modifier(utilisateur.getId(), id, dto);
    }
}
