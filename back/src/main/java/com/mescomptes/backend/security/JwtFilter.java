package com.mescomptes.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cette classe va intercepter chaque requête.
 * Elle s'exécute avant chaque requête et lit le header "Authorization"
 * afin de valider le token et injecter l'utilisateur dans le contexte de sécurité.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    // Attribut de requête lu par ReponsesSecurite pour expliquer le 401
    static final String JETON_REFUSE = JwtFilter.class.getName() + ".refus";

    record Refus(String code, String message) {
    }

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // Si pas de header Authorization ou qu'il ne commence pas par "Bearer ", on passe
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // On extrait le token (après "Bearer ")
        final String token = authHeader.substring(7);
        // Jeton refusé : la requête continue sans utilisateur. Une route publique répond normalement,
        // une route protégée répond 401 (ReponsesSecurite), au lieu de laisser l'exception finir en 500.
        try {
            // Une seule lecture du jeton : signature et expiration y sont vérifiées
            final String identifiant = jwtService.extractIdentifiant(token);
            if (identifiant == null || identifiant.isBlank()) {
                throw new MalformedJwtException("Jeton sans identifiant");
            }

            // Si l'utilisateur n'est pas déjà authentifié
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifiant);

                // On crée le token d'authentification Spring Security
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // On l'injecte dans le contexte — Spring Security considère l'user comme connecté
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute(JETON_REFUSE, new Refus("TOKEN_EXPIRED", "Votre session a expiré, veuillez vous reconnecter."));
        } catch (JwtException | IllegalArgumentException e) {
            // Malformé, signature fausse (secret changé), vide ou sans identifiant
            request.setAttribute(JETON_REFUSE, new Refus("INVALID_TOKEN", "Votre jeton de connexion n'est pas valide, veuillez vous reconnecter."));
        } catch (UsernameNotFoundException e) {
            request.setAttribute(JETON_REFUSE, new Refus("INVALID_TOKEN", "Ce compte n'existe plus, veuillez vous reconnecter."));
        }

        filterChain.doFilter(request, response);
    }
}
