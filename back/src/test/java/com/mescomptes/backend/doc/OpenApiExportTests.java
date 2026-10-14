package com.mescomptes.backend.doc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exporte la description OpenAPI de l'API dans docs/api/openapi.json, sans démarrer de serveur.
 * Ne tourne que sur demande, pour ne pas réécrire le fichier à chaque lancement des tests :
 * ./mvnw test -Dtest=OpenApiExportTests -Dopenapi.export=true
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "openapi.export", matches = "true")
class OpenApiExportTests {

    // Les tests Maven tournent depuis back/
    private static final Path FICHIER = Path.of("..", "docs", "api", "openapi.json");

    @Autowired
    private MockMvc mvc;

    @Test
    void exporterLaDescriptionOpenApi() throws Exception {
        byte[] description = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        Files.createDirectories(FICHIER.getParent());
        Files.write(FICHIER, description);
        assertThat(FICHIER).isNotEmptyFile();
    }
}
