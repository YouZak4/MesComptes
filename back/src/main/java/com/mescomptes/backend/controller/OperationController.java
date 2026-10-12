package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.CustomUserDetails;
import com.mescomptes.backend.model.dto.DtoOperation;
import com.mescomptes.backend.model.dto.DtoOperationSaisie;
import com.mescomptes.backend.model.dto.FiltreOperations;
import com.mescomptes.backend.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    // Filtres facultatifs : ?periodeId=&compteId=&categorieId=&libelleId=&q=
    @GetMapping
    public List<DtoOperation> lister(@AuthenticationPrincipal CustomUserDetails utilisateur, FiltreOperations filtre) {
        return operationService.lister(utilisateur.getId(), filtre);
    }

    @PostMapping
    public ResponseEntity<DtoOperation> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                              @Valid @RequestBody DtoOperationSaisie dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operationService.creer(utilisateur.getId(), dto));
    }

    @PutMapping("/{id}")
    public DtoOperation modifier(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                 @PathVariable Long id, @Valid @RequestBody DtoOperationSaisie dto) {
        return operationService.modifier(utilisateur.getId(), id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal CustomUserDetails utilisateur, @PathVariable Long id) {
        operationService.supprimer(utilisateur.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
