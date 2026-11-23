# MesComptes

Application web de suivi des dépenses : plusieurs comptes par utilisateur, périodes mensuelles réglables, catégories et libellés, opérations récurrentes générées chaque nuit, tableau de bord par période.

## Organisation

| Dossier | Contenu | Consignes |
|---|---|---|
| `back/` | API Spring Boot 4, Java 21, PostgreSQL, Flyway | `back/CLAUDE.md` |
| `front/` | Vue 3 + TypeScript, Vite, Pinia, pnpm | `front/CLAUDE.md` |
| `docs/` | Conception, ADR, API, charte graphique | `docs/README.md` |

Le front appelle l'API sur `http://localhost:8085/api` avec un JWT dans `Authorization: Bearer`.

## Langue

Tout est en français : code (classes, méthodes, variables), commentaires, messages d'erreur, docs et commits. Garder ce choix pour tout nouveau code (`creer`, `lister`, `supprimer`…). Les fichiers sont en UTF-8.

## Documentation de référence

À lire avant de toucher à une règle métier :
- `docs/conception/conception-mescomptes.md` : périmètre V1 et règles métier.
- `docs/conception/objets-back.md` : entités et DTO.
- `docs/api/README.md` : conventions de l'API (codes d'erreur, formats, archivage, 404 pour les données d'un autre utilisateur).
- `docs/visuel/charte-graphique.md` : charte « Sapin » pour tout ce qui est visuel.
- `docs/adr/` : un ADR accepté ne se modifie jamais, on en écrit un nouvel qui le remplace.

## Règles

- **Doc dans le même commit** : toute modification de l'API ou d'une règle métier met à jour `docs/` dans le même commit, et régénère `docs/api/openapi.json` (commande dans `back/CLAUDE.md`).
- **Contrôles avant de dire « terminé »** : `./mvnw test` dans `back/`, `pnpm build` et `pnpm lint` dans `front/`. Si un contrôle ne peut pas tourner (PostgreSQL absent…), le dire.
- **Jamais de secret** dans le code ou la doc. `back/src/main/resources/application.properties` et `front/.env*` sont ignorés par Git ; seuls les modèles (`.example`) sont versionnés.

## Git

- Branche principale : `develop` (`master` existe aussi). Le dépôt est sur GitHub, les changements passent par des pull requests.
- **Une branche par fonctionnalité** : chaque nouvelle fonctionnalité ou modification se fait sur sa propre branche créée depuis `develop` (`feat/…`, `fix/…`, `docs/…`, `refactor/…`). Jamais de travail directement sur `develop` ou `master`.
- **Pas de commit sans demande explicite** : ne jamais commiter, pousser ni ouvrir de PR tant que l'utilisateur ne l'a pas demandé.
- Ne **pas** ajouter de ligne `Co-Authored-By` dans les commits.
- Messages au format commitlint (`@commitlint/config-conventional`) : `type(portée): sujet`
  - types autorisés : `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test` ;
  - portée facultative : `back`, `front`, `visuel`… ;
  - sujet en français, en minuscule, sans point final, court.
  - Exemples du dépôt : `feat(back): créer les périodes manquantes avant la première`, `fix(front): l'anneau garde sa place quand « Autres » est déplié`, `docs: calendrier, libellés de la catégorie et « Autres » dépliable`.

## Avant de pousser une branche

Ne jamais merger soi-même dans `develop` : l'intégration passe par une pull request. Une branche qui contient une nouvelle fonctionnalité n'est poussée **que si les quatre étapes ci-dessous sont passées**. Si une étape échoue, corriger puis reprendre depuis l'étape 1. Présenter le résultat de chaque étape à l'utilisateur avant de pousser.

1. **Tests unitaires et d'intégration** : lancer `./mvnw test` dans `back/` (plus `pnpm build` et `pnpm lint` dans `front/`). Si la fonctionnalité n'est pas couverte, écrire les tests manquants d'abord (règles de `back/CLAUDE.md` : cas nominal, validation, 404 sur les données de `autre`). Le front n'a pas de framework de test : proposer d'en ajouter un (Vitest) plutôt que de l'installer sans accord.
2. **Revue de code** : relire tout le diff de la branche par rapport à `develop` (`git diff develop...`) : bugs, respect des conventions des `CLAUDE.md`, doc `docs/` et `openapi.json` à jour, code mort ou dupliqué. Corriger ce qui doit l'être.
3. **Tests fonctionnels et non-régression** : lancer l'application (back + front) et tester la nouvelle fonctionnalité, puis les fonctionnalités existantes qu'elle peut toucher, pour repérer les effets de bord. Mettre à jour le cahier de test `docs/tests/cahier-de-test.md` (le créer s'il n'existe pas) : pour chaque cas, l'écran ou la route, les étapes, le résultat attendu, le résultat obtenu et la date.
4. **Recherche de vulnérabilités** : analyser le code de la branche sous l'angle sécurité : isolation des données par `utilisateurId` (404 pour les données d'un autre), contrôle d'accès et JWT, validation des entrées, injections (SQL/JPQL, XSS côté Vue avec `v-html`), secrets exposés, dépendances ajoutées. Corriger toute faille trouvée.
