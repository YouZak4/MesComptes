package com.mescomptes.backend.controller;

import com.mescomptes.backend.model.dto.DtoInscriptionEnAttente;
import com.mescomptes.backend.model.dto.DtoLogin;
import com.mescomptes.backend.model.dto.DtoRegister;
import com.mescomptes.backend.model.dto.DtoRenvoiCode;
import com.mescomptes.backend.model.dto.DtoUtilisateur;
import com.mescomptes.backend.model.dto.DtoVerification;
import com.mescomptes.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentification", description = "Inscription vérifiée par code envoyé par mail, puis connexion (routes publiques)")
@SecurityRequirements // routes publiques : pas de JWT
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Démarre une inscription", description = "Envoie un code à 6 chiffres par mail. Une adresse déjà inscrite reçoit une alerte au lieu du code, avec la même réponse.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Inscription en attente du code")
    })
    @PostMapping("/register")
    public ResponseEntity<DtoInscriptionEnAttente> register(@Valid @RequestBody DtoRegister request) {
        return ResponseEntity.accepted().body(authService.register(request));   // 202 : pas encore créé
    }

    @Operation(summary = "Relit une inscription en attente", description = "Pour réafficher la saisie du code après un rafraîchissement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscription en attente"),
            @ApiResponse(responseCode = "400", description = "Jeton inconnu ou inscription trop ancienne")
    })
    @GetMapping("/register/{jeton}")
    public ResponseEntity<DtoInscriptionEnAttente> getInscriptionEnAttente(@PathVariable String jeton) {
        return ResponseEntity.ok(authService.getInscriptionEnAttente(jeton));
    }

    @Operation(summary = "Valide le code et crée l'utilisateur", description = "Crée aussi un premier compte « Compte courant ».")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé"),
            @ApiResponse(responseCode = "400", description = "Code incorrect, expiré, ou trop de tentatives"),
            @ApiResponse(responseCode = "409", description = "Identifiant ou adresse pris entre-temps")
    })
    @PostMapping("/register/verify")
    public ResponseEntity<DtoUtilisateur> verify(@Valid @RequestBody DtoVerification request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.verify(request));
    }

    @Operation(summary = "Renvoie un nouveau code", description = "L'ancien code ne marche plus. Un envoi par minute au plus.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Nouveau code envoyé"),
            @ApiResponse(responseCode = "429", description = "Délai de renvoi pas encore écoulé")
    })
    @PostMapping("/register/resend")
    public ResponseEntity<DtoInscriptionEnAttente> resend(@Valid @RequestBody DtoRenvoiCode request) {
        return ResponseEntity.accepted().body(authService.renvoyerCode(request));
    }

    @Operation(summary = "Se connecte et obtient le JWT", description = "Le corps de la réponse est le jeton, en texte. Rattrape au passage les récurrences en retard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Jeton JWT, valable 24 heures"),
            @ApiResponse(responseCode = "401", description = "Identifiant ou mot de passe incorrect")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody DtoLogin request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
