package com.mescomptes.backend.security;

import com.mescomptes.backend.model.CustomUserDetails;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

//Génère et valide les tokens
@Service
public class JwtService {

    private final long expiration;
    // Construits une seule fois : le parseur sert à chaque requête authentifiée
    private final SecretKey signingKey;
    private final JwtParser parser;

    // Lit la clé et la durée de validité depuis application.properties
    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        this.expiration = expiration;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    // Génère un token pour un utilisateur
    public String generateToken(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userPrincipal)) {
            throw new IllegalArgumentException("Principal is not CustomUserDetails");
        }

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())                             // identifiant dans le token
                .claim("roles", roles)                                    // rôles dans le payload
                .issuedAt(new Date())                                           // date de création
                .expiration(new Date(System.currentTimeMillis() + expiration))  // date d'expiration
                .signWith(signingKey)
                .compact();
    }

    // Vérifie la signature et l'expiration, puis rend l'identifiant (sujet) du jeton.
    // Lève ExpiredJwtException si le jeton a expiré, JwtException s'il est malformé ou mal signé.
    public String extractIdentifiant(String token) {
        return parser.parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
