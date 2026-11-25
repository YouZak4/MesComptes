# Back MesComptes (Spring Boot)

Conventions de code, base de données et tests : sections « Conventions du back » de `CONTRIBUTING.md` (importé par le `CLAUDE.md` racine). Lancement et configuration : `back/README.md`.

## Commandes (depuis `back/`)

```bash
./mvnw spring-boot:run          # API sur http://localhost:8085/api
./mvnw test                     # tous les tests (PostgreSQL requis)
./mvnw test -Dtest=OpenApiExportTests -Dopenapi.export=true   # régénère docs/reference/api/openapi.json
```

Swagger UI : <http://localhost:8085/api/swagger-ui.html>

## Configuration

- `src/main/resources/application.properties` est ignoré par Git : il se crée depuis `application.properties.example`. Ne jamais le commiter ni recopier ses valeurs.
- Context path `/api`, port 8085, base PostgreSQL `mescomptes`.
- Planifications (cron) : génération des récurrences à 0h05, nettoyage des inscriptions en attente toutes les heures.
