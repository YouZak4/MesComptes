package com.mescomptes.backend.config;

import com.mescomptes.backend.model.dto.DtoApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Description OpenAPI de l'API, servie par springdoc :
 * JSON sur /api/v3/api-docs, interface Swagger UI sur /api/swagger-ui.html.
 * Toutes les routes exigent le JWT, sauf celles marquées sans sécurité (inscription et connexion).
 */
@Configuration
public class OpenApiConfig {

    public static final String JWT = "jwt";
    private static final String ERREUR = "DtoApiError";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MesComptes API")
                        .version("v1")
                        .description("Suivi des dépenses : comptes, périodes, catégories et libellés, opérations, "
                                + "récurrences et tableau de bord. Une ressource d'un autre utilisateur répond 404. "
                                + "Les erreurs suivent toujours le format DtoApiError."))
                // Adresse relative : vaut pour toute installation, et le fichier exporté ne dépend pas de la machine
                .servers(List.of(new Server().url("/api")))
                .components(new Components().addSecuritySchemes(JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Jeton renvoyé par POST /auth/login, valable 24 heures.")))
                .addSecurityItem(new SecurityRequirement().addList(JWT));
    }

    /**
     * Complète chaque route avec les réponses communes, que les annotations ne disent pas :
     * les erreurs ont le format DtoApiError (springdoc leur donnerait sinon le schéma de la réponse normale),
     * un 204 n'a pas de corps, et chaque route reçoit les erreurs qui la concernent (validation, jeton, rôle, introuvable).
     */
    @Bean
    public OpenApiCustomizer reponsesCommunes() {
        return openApi -> {
            ModelConverters.getInstance().read(DtoApiError.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().forEach((chemin, item) -> item.readOperationsMap().forEach((methode, operation) -> {
                ApiResponses reponses = operation.getResponses();
                if (operation.getRequestBody() != null) {
                    reponses.putIfAbsent("400", erreur("Corps invalide : champ obligatoire manquant, format ou longueur"));
                }
                if (!estPublique(operation)) {
                    reponses.putIfAbsent("401", erreur("JWT absent, invalide ou expiré (AUTH_REQUIRED, INVALID_TOKEN, TOKEN_EXPIRED)"));
                    reponses.putIfAbsent("403", erreur("Utilisateur connecté sans le rôle requis (ACCESS_DENIED)"));
                }
                if (chemin.contains("{id}")) {
                    reponses.putIfAbsent("404", erreur("Introuvable, ou appartient à un autre utilisateur"));
                }
                reponses.forEach((code, reponse) -> {
                    if (code.equals("204")) {
                        reponse.setContent(null);
                    } else if (code.compareTo("400") >= 0) {
                        reponse.setContent(contenuErreur());
                    }
                });
            }));
        };
    }

    // Les routes publiques déclarent une liste de sécurité vide (@SecurityRequirements sans argument)
    private static boolean estPublique(Operation operation) {
        return operation.getSecurity() != null && operation.getSecurity().isEmpty();
    }

    private static ApiResponse erreur(String description) {
        return new ApiResponse().description(description).content(contenuErreur());
    }

    private static Content contenuErreur() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERREUR)));
    }
}
