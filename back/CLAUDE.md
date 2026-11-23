# Back MesComptes (Spring Boot)

## Commandes (depuis `back/`)

```bash
./mvnw spring-boot:run          # API sur http://localhost:8085/api
./mvnw test                     # tous les tests (PostgreSQL requis)
./mvnw test -Dtest=OpenApiExportTests -Dopenapi.export=true   # régénère docs/api/openapi.json
```

Swagger UI : http://localhost:8085/api/swagger-ui.html

## Configuration

- `src/main/resources/application.properties` est ignoré par Git : il se crée depuis `application.properties.example`. Ne jamais le commiter ni recopier ses valeurs.
- Context path `/api`, port 8085, base PostgreSQL `mescomptes`.
- Planifications (cron) : génération des récurrences à 0h05, nettoyage des inscriptions en attente toutes les heures.

## Architecture (`com.mescomptes.backend`)

`controller` → `service` → `repository` (Spring Data JPA), avec `model/entities`, `model/dto`, `model/enums`, `security` (JWT), `exception`, `config`.

- **Contrôleurs** fins : ils récupèrent l'utilisateur avec `@AuthenticationPrincipal CustomUserDetails`, valident avec `@Valid` et délèguent au service. Chaque route porte `@Operation` (et `@ApiResponses` si besoin) pour springdoc.
- **Services** : `@Transactional` (`readOnly = true` en lecture), Javadoc en français, méthodes nommées en français (`lister`, `creer`, `modifier`, `supprimer`, `trouver`).
- **Isolation des données** : toujours filtrer par `utilisateurId` (`findByIdAndUtilisateurId`…). Une ressource d'un autre utilisateur lève `ResourceNotFoundException` (404), jamais 403.
- **DTO** : `DtoXxx` en sortie, `DtoXxxSaisie` en entrée. Les entités ne sortent jamais des contrôleurs.
- **Lombok** : `@Getter @Setter @NoArgsConstructor` sur les entités, `@RequiredArgsConstructor` pour l'injection.
- **Erreurs** : lever les exceptions de `exception/` (`ResourceNotFoundException`, `BusinessRuleException` → 422, `AlreadyExist` → 409…) ; `GlobalExceptionHandler` produit le `DtoApiError`. Messages en français, affichables tels quels.
- **Retirer** un élément : il est supprimé s'il n'a jamais servi (204), archivé sinon (200 avec l'élément).
- **Montants** : `BigDecimal`, toujours positifs ; le sens vient du `TypeOperation` de la catégorie.
- Les catégories et libellés **fournis** (communs à tous) sont posés par `DonneesFourniesInitializer` ; ils ne se modifient pas, ils se masquent.

## Base de données : pièges

- Hibernate est en `ddl-auto=update` : il crée et complète les tables seul.
- **Flyway** (`db/migration/V<n>__description.sql`) ne sert que pour ce qu'Hibernate ne sait pas faire : renommages, changements de type, reprise de données. Flyway passe avant Hibernate, donc une migration doit fonctionner sur une base neuve où la table n'existe pas encore (voir le garde `IF NOT EXISTS … RETURN` de `V3__icones_lucide.sql`).
- Ne jamais modifier une migration déjà commitée : en ajouter une nouvelle.

## Tests

- Tests d'API dans `controller/`, ils héritent de `ApiTestBase` : `@SpringBootTest` + MockMvc sur la vraie base PostgreSQL, chaque test est annulé (`@Transactional`), deux utilisateurs neufs (`moi`, `autre`), `MailService` mocké.
- Utiliser `en(utilisateur, requete)` pour une requête authentifiée et les raccourcis `compte(...)`, `operation(...)`, `categorieFournie(...)`.
- Toute nouvelle route : tester le cas nominal, la validation et l'accès refusé aux données de `autre` (404).
