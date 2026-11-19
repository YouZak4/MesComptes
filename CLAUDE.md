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
- Pour chaque modification : créer une nouvelle branche depuis `develop` (`feat/…`, `fix/…`, `docs/…`, `refactor/…`) puis commiter. Ne pas pousser ni ouvrir de PR sans demande.
- Ne **pas** ajouter de ligne `Co-Authored-By` dans les commits.
- Messages au format commitlint (`@commitlint/config-conventional`) : `type(portée): sujet`
  - types autorisés : `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test` ;
  - portée facultative : `back`, `front`, `visuel`… ;
  - sujet en français, en minuscule, sans point final, court.
  - Exemples du dépôt : `feat(back): créer les périodes manquantes avant la première`, `fix(front): l'anneau garde sa place quand « Autres » est déplié`, `docs: calendrier, libellés de la catégorie et « Autres » dépliable`.
