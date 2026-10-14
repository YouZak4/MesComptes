package com.mescomptes.backend.security;

import com.mescomptes.backend.model.dto.DtoApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Réponses de Spring Security au même format que les autres erreurs (DtoApiError) :
 * 401 sans jeton valable sur une route protégée, 403 si l'utilisateur connecté n'a pas le rôle requis.
 * Le front déconnecte sur 401 : c'est voulu ici, la session n'est plus utilisable.
 */
@Component
@RequiredArgsConstructor
public class ReponsesSecurite implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        // Posé par JwtFilter quand un jeton a été envoyé mais refusé
        if (request.getAttribute(JwtFilter.JETON_REFUSE) instanceof JwtFilter.Refus refus) {
            ecrire(request, response, HttpStatus.UNAUTHORIZED, refus.code(), refus.message());
        } else {
            ecrire(request, response, HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Vous devez être connecté pour accéder à cette ressource.");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        ecrire(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Vous n'avez pas accès à cette ressource.");
    }

    private void ecrire(HttpServletRequest request, HttpServletResponse response, HttpStatus statut, String code, String message) throws IOException {
        response.setStatus(statut.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new DtoApiError(statut, code, message, request.getRequestURI()));
    }
}
