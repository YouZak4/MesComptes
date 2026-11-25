# MesComptes

Application web de suivi des dépenses : plusieurs comptes par utilisateur, périodes mensuelles réglables, catégories et libellés, opérations récurrentes générées chaque nuit, tableau de bord par période.

## Organisation

| Dossier | Contenu | Consignes |
|---|---|---|
| `back/` | API Spring Boot 4, Java 21, PostgreSQL, Flyway | `back/CLAUDE.md` |
| `front/` | Vue 3 + TypeScript, Vite, Pinia, pnpm | `front/CLAUDE.md` |
| `docs/` | Conception, ADR, API, charte graphique, cahier de test | `docs/README.md` |

Le front appelle l'API sur `http://localhost:8085/api` avec un JWT dans `Authorization: Bearer`.

## Règles du projet

Les règles communes à tous les contributeurs (langue, branches, commits, quatre étapes avant de pousser, documentation, secrets, conventions de code du back et du front) sont dans `CONTRIBUTING.md`, importé ici. Elles s'appliquent telles quelles.

@CONTRIBUTING.md

## Documentation de référence

À lire avant de toucher à une règle métier :

- `docs/explanation/conception-mescomptes.md` : périmètre V1 et règles métier.
- `docs/reference/back/objets-back.md` : entités et DTO.
- `docs/reference/api/README.md` : conventions de l'API (codes d'erreur, formats, archivage, 404 pour les données d'un autre utilisateur).
- `docs/reference/visuel/charte-graphique.md` : charte « Sapin » pour tout ce qui est visuel.
- `docs/adr/` : décisions d'architecture.
- `docs/how-to/` : marches à suivre (ajouter une route, écrire une migration Flyway, régénérer `openapi.json`).

## Consignes propres à Claude

- **Pas de commit sans demande explicite** : ne jamais commiter, pousser ni ouvrir de PR tant que l'utilisateur ne l'a pas demandé.
- Ne **pas** ajouter de ligne `Co-Authored-By` dans les commits.
- **Contrôles avant de dire « terminé »** : `./mvnw test` dans `back/`, `pnpm test`, `pnpm build` et `pnpm lint` dans `front/`. Si un contrôle ne peut pas tourner (PostgreSQL absent…), le dire.
- **Avant de pousser** : suivre les quatre étapes de `CONTRIBUTING.md` et présenter le résultat de chaque étape à l'utilisateur avant de pousser.
- Toute règle qui concerne aussi un développeur humain va dans `CONTRIBUTING.md` ou dans `docs/`, pas ici.
