package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoRecurrence;
import com.mescomptes.backend.model.dto.DtoRecurrenceSaisie;
import com.mescomptes.backend.service.RecurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recurrences")
@RequiredArgsConstructor
public class RecurrenceController {

    private final RecurrenceService recurrenceService;

    @GetMapping
    public List<DtoRecurrence> lister(@AuthenticationPrincipal CustomUserDetails utilisateur) {
        return recurrenceService.lister(utilisateur.getId());
    }

    @PostMapping
    public ResponseEntity<DtoRecurrence> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                               @Valid @RequestBody DtoRecurrenceSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurrenceService.creer(utilisateur.getId(), dto));
    }

    // Ne touche que les occurrences futures
    @PutMapping("/{id}")
    public DtoRecurrence modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                  @PathVariable Long id, @Valid @RequestBody DtoRecurrenceSaisie dto) {
        return recurrenceService.modifier(utilisateur.getId(), id, dto);
    }

    // 200 avec la récurrence arrêtée (historique conservé), 204 si elle n'avait rien généré et a été supprimée
    @DeleteMapping("/{id}")
    public ResponseEntity<DtoRecurrence> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        return recurrenceService.supprimer(utilisateur.getId(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
