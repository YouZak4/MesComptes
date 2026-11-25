# Contribuer à MesComptes

Ce guide réunit les règles communes à toute modification du projet : langue, Git, contrôles avant de pousser, documentation et conventions de code. Pour installer et lancer le projet, voir le [README](README.md).

## Langue

Tout est en français : code (classes, méthodes, variables), commentaires, messages d'erreur, documentation et commits. Les nouveaux noms suivent ce choix (`creer`, `lister`, `supprimer`…). Les fichiers sont en UTF-8.

## Branches et pull requests

- Le dépôt est sur GitHub. La branche principale est `develop` (`master` existe aussi).
- **Une branche par modification**, créée depuis `develop` et préfixée par son type : `feat/…`, `fix/…`, `docs/…`, `refactor/…`.
- On ne travaille jamais directement sur `develop` ni sur `master`, et on ne merge jamais soi-même dans `develop` : l'intégration passe par une pull request.

## Messages de commit

Format commitlint (`@commitlint/config-conventional`) : `type(portée): sujet`.

- **Types autorisés :** `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`.
- **Portée** facultative : `back`, `front`, `visuel`…
- **Sujet** en français, en minuscule, court, sans point final.

Exemples tirés du dépôt :

```text
feat(back): créer les périodes manquantes avant la première
fix(front): l'anneau garde sa place quand « Autres » est déplié
docs: calendrier, libellés de la catégorie et « Autres » dépliable
```

## Avant de pousser une branche

Une branche qui apporte une fonctionnalité n'est poussée que si les quatre étapes ci-dessous sont passées. Si une étape échoue, corriger puis reprendre à l'étape 1.

1. **Tests unitaires et d'intégration.** `./mvnw test` dans `back/` ; `pnpm test`, `pnpm build` et `pnpm lint` dans `front/`. Si la fonctionnalité n'est pas couverte, écrire d'abord les tests manquants (voir [Tests du back](#tests-du-back) et [Tests du front](#tests-du-front)). Si un contrôle ne peut pas tourner (PostgreSQL absent…), le signaler dans la PR.
2. **Revue de code.** Relire tout le diff de la branche (`git diff develop...`) : bugs, respect de ce guide, documentation et `openapi.json` à jour, code mort ou dupliqué.
3. **Tests fonctionnels et de non-régression.** Lancer le back et le front, tester la nouvelle fonctionnalité puis celles qu'elle peut toucher. Consigner la campagne dans un nouveau fichier `docs/tests/campagnes/AAAA-MM-JJ-<n°>-<branche>.md`, copié du [modèle](docs/tests/campagnes/_modele.md) : pour chaque cas, l'écran ou la route, les étapes, le résultat attendu et le résultat obtenu. Ajouter ensuite une ligne en tête du [cahier de test](docs/tests/cahier-de-test.md).
4. **Recherche de vulnérabilités.** Relire la branche sous l'angle sécurité : isolation des données par `utilisateurId` (404 pour les données d'un autre), contrôle d'accès et JWT, validation des entrées, injections (SQL/JPQL, XSS avec `v-html` côté Vue), secrets exposés, dépendances ajoutées.

## Documentation

- **Doc dans le même commit.** Toute modification de l'API ou d'une règle métier met à jour `docs/` dans le même commit, et régénère `docs/reference/api/openapi.json` ([marche à suivre](docs/how-to/back/regenerer-openapi.md)).
- Les schémas s'écrivent en Mermaid, directement dans le Markdown.
- Un ADR accepté ne se modifie jamais : on en écrit un nouveau qui le remplace (voir [`docs/adr/`](docs/adr/README.md)).
- Chaque document de `docs/reference/`, `docs/explanation/` et `docs/how-to/` commence par un en-tête qui donne sa date de dernière relecture. On la met à jour à chaque fois qu'on relit le document contre le code, même sans rien y changer :

  ```yaml
  ---
  derniere_relecture: 2026-11-25
  ---
  ```

- La CI vérifie la syntaxe Markdown, les liens et `openapi.json` à chaque pull request (voir [Contrôles automatiques](#contrôles-automatiques)).
- Organisation de la documentation : [`docs/README.md`](docs/README.md).

## Contrôles automatiques

Le workflow `.github/workflows/docs.yml` tourne sur chaque pull request et à chaque push sur `develop` et `master` :

| Contrôle | Outil | Échoue si… |
|---|---|---|
| Syntaxe Markdown | markdownlint (règles dans `.markdownlint-cli2.yaml`) | un fichier `.md` ne respecte pas les règles |
| Liens internes | lychee, hors ligne | un lien relatif ou une ancre `#…` ne mène nulle part |
| Description de l'API | export OpenAPI sur une base PostgreSQL de CI | `openapi.json` régénéré diffère de celui commité |

Pour corriger une erreur markdownlint, lire la règle citée (`MD032`…) dans la [documentation de markdownlint](https://github.com/DavidAnson/markdownlint/blob/main/doc/Rules.md).

## Secrets

Jamais de mot de passe, de clé ni de secret dans le code ou la documentation. `back/src/main/resources/application.properties` et `front/.env*` sont ignorés par Git ; seuls les modèles (`.example`) sont versionnés.

## Conventions du back

Pas à pas : [ajouter une route](docs/how-to/back/ajouter-une-route.md), [écrire une migration Flyway](docs/how-to/back/ecrire-une-migration-flyway.md).

Architecture en couches dans `com.mescomptes.backend` : `controller` → `service` → `repository` (Spring Data JPA), avec `model/entities`, `model/dto`, `model/enums`, `security` (JWT), `exception` et `config`.

- **Contrôleurs fins** : ils récupèrent l'utilisateur avec `@AuthenticationPrincipal CustomUserDetails`, valident avec `@Valid` et délèguent au service. Chaque route porte `@Operation` (et `@ApiResponses` si besoin) pour springdoc.
- **Services** : `@Transactional` (`readOnly = true` en lecture), Javadoc en français, méthodes nommées en français (`lister`, `creer`, `modifier`, `supprimer`, `trouver`).
- **Isolation des données** : toujours filtrer par `utilisateurId` (`findByIdAndUtilisateurId`…). Une ressource d'un autre utilisateur lève `ResourceNotFoundException` (404), jamais 403.
- **DTO** : `DtoXxx` en sortie, `DtoXxxSaisie` en entrée. Les entités ne sortent jamais des contrôleurs.
- **Lombok** : `@Getter @Setter @NoArgsConstructor` sur les entités (jamais `@Data`), `@RequiredArgsConstructor` pour l'injection.
- **Erreurs** : lever les exceptions de `exception/` (`ResourceNotFoundException` → 404, `BusinessRuleException` → 422, `AlreadyExist` → 409…) ; `GlobalExceptionHandler` produit le `DtoApiError`. Messages en français, affichables tels quels.
- **Retirer un élément** : il est supprimé s'il n'a jamais servi (204), archivé sinon (200 avec l'élément).
- **Montants** : `BigDecimal`, toujours positifs ; le sens vient du `TypeOperation` de la catégorie.
- **Éléments fournis** : les catégories et libellés communs à tous sont posés par `DonneesFourniesInitializer`. Ils ne se modifient pas, ils se masquent.

### Base de données

- **Toute modification du schéma ou des données passe par Flyway** (`db/migration/V<n>__description.sql`), dans le même commit que l'entité modifiée ([ADR 0008](docs/adr/0008-toute-modification-de-schema-par-flyway.md)).
- Hibernate est en `ddl-auto=validate` : il ne modifie jamais la base, il vérifie que les entités correspondent au schéma et refuse de démarrer sinon.
- On ne modifie jamais une migration déjà commitée : on en ajoute une nouvelle.
- Contraintes et index portent un nom explicite (`fk_operation_compte`, `uk_categorie_nom`, `idx_operation_compte_date`).
- Marche à suivre : [écrire une migration Flyway](docs/how-to/back/ecrire-une-migration-flyway.md).

### Tests du back

- Les tests d'API sont dans `controller/` et héritent de `ApiTestBase` : `@SpringBootTest` + MockMvc sur la vraie base PostgreSQL, chaque test annulé (`@Transactional`), deux utilisateurs neufs (`moi` et `autre`), `MailService` mocké.
- Utiliser `en(utilisateur, requete)` pour une requête authentifiée et les raccourcis `compte(...)`, `operation(...)`, `categorieFournie(...)`.
- Toute nouvelle route est testée sur trois cas : le cas nominal, la validation, et l'accès refusé aux données de `autre` (404).

## Conventions du front

Architecture : [docs/reference/front/architecture-front.md](docs/reference/front/architecture-front.md).

- `<script setup lang="ts">`, Composition API uniquement.
- Les composants n'appellent jamais axios directement : ils passent par les services de `src/services/`.
- Erreurs d'API : afficher `messageErreur(e, "message par défaut")` (`utils/erreurs.ts`) ; le back renvoie déjà un message français affichable.
- Un 401 déconnecte l'utilisateur (intercepteur de `api.ts`). C'est pourquoi le back répond 422, et non 401, à un mot de passe actuel faux.
- Icônes : `@lucide/vue` uniquement.
- Style : utiliser les variables de `styles/theme.css` (`--vert-700`, `--rayon-m`…), jamais de couleur en dur. Tout nouveau composant suit la [charte graphique](docs/reference/visuel/charte-graphique.md).
- Nommage : écrans `xxxView.vue` dans `views/`, composants `XxxComponent.vue` dans `components/`, stores Pinia en syntaxe setup (`defineStore("nom", () => { … })`).
- Prettier : 4 espaces, guillemets doubles, point-virgule, 80 colonnes, fins de ligne LF.
- Textes de l'interface en français, avec les guillemets « ».

### Tests du front

- Vitest, configuré dans le bloc `test` de `vite.config.ts` : environnement `jsdom`, fichiers `src/**/*.test.ts`.
- Un test se place à côté du fichier testé : `src/utils/format.ts` → `src/utils/format.test.ts`.
- Importer `describe`, `it` et `expect` depuis `vitest` (pas de globales). Noms des tests en français.
- Composants : `@vue/test-utils` (`mount`).
- Tout nouveau code testable (utilitaire, store, composable) arrive avec ses tests.

### Dépendances du front

- Versions exactes dans `package.json`, sans `^` ni `~` : `saveExact: true` dans `pnpm-workspace.yaml` le fait pour `pnpm add`.
- `pnpm-lock.yaml` est versionné, ne se modifie jamais à la main et part dans le même commit que `package.json`.
- Versions de pnpm (`packageManager`) et de Node (`.nvmrc`, `engines`) fixées ; la version majeure de `@types/node` suit celle de Node.
- TypeScript reste en 6.x : la 7 (compilateur en Go) n'a pas l'API JavaScript dont `vue-tsc` et `typescript-eslint` ont besoin. À revoir quand ils la prendront en charge.
- Pinia 4 demande `@vue/devtools-api` installé à côté de lui, dans la version attendue par ses peers.
