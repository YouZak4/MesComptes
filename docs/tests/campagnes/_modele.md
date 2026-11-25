# Campagne du AAAA-MM-JJ (n°) : `type/nom-de-la-branche`

Légende et autres campagnes : [cahier de test](../cahier-de-test.md).

Changements testés : ce que la branche modifie. Risque d'effet de bord : les écrans ou routes qui peuvent être touchés.

Environnement : versions utiles (Node, pnpm, Java), back et front lancés sur quelle branche, navigateur.

## Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ N tests, 0 échec |
| `pnpm test` | ✅ N tests |
| `pnpm lint`, `pnpm build` | ✅ |
| `docs/reference/api/openapi.json` | ✅ régénéré, ou inchangé (API non modifiée) |

## Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| 1 | `/` | Ce que l'on fait | Ce qui doit se passer | Ce qui s'est passé | ✅ |

## Bilan

Régressions constatées ou non, et données de test nettoyées.
