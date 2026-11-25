# Campagne du 2026-11-24 (5) : `chore/versions-majeures-front`

Légende et autres campagnes : [cahier de test](../cahier-de-test.md).

Changements testés : Pinia 3.0.4 → 4.1.0 (avec `@vue/devtools-api` 8.2.1 installé explicitement, comme Pinia 4 l'exige), TypeScript 5.9.3 → 6.0.3, `@types/node` 25.5.0 → 26.6.6 et projet fixé sur Node 26 (`.nvmrc`, `engines`) ; `baseUrl` retiré des tsconfig (obsolète en TypeScript 6). TypeScript 7.0.2 essayé puis écarté : `pnpm build` (vue-tsc) et `pnpm lint` (typescript-eslint) échouent, faute d'API JavaScript. Risque d'effet de bord : tout ce qui passe par les stores Pinia (auth, profil, notification, saisie) et le contrôle de types.

Environnement : Node 26.13.1 (fnm) et pnpm 11.23.0, serveur front relancé sur la branche (Pinia v4.1.0 vérifié dans le cache de dépendances de Vite), back inchangé sur `develop`, Chrome.

## Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 100 tests, 0 échec, 1 ignoré (export OpenAPI) ; back non modifié |
| `pnpm test` (Node 26) | ✅ 117 tests |
| `pnpm lint`, `pnpm build` (Node 26) | ✅ |
| `tsc -p tsconfig.node.json --noEmit` | ✅ (`vite.config.ts`, hors du contrôle de `pnpm build`) |
| `pnpm peers check` | ✅ aucun conflit |
| `pnpm audit` | 2 alertes moyennes, déjà connues (js-beautify via `@vue/test-utils`, tests uniquement) |

## Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| 4 | `/` | Ouvrir le tableau de bord | Novembre 2026, chiffres identiques à la campagne (4) | Solde -294,51 €, dépenses 408,53 € | ✅ |
| 5 | `/` | Période précédente, puis « Période en cours » | Octobre puis Novembre 2026 | Conforme | ✅ |
| 6 | Saisie rapide | Touche N, 1,23 € en « Autres dépenses », note de test | « Opération enregistrée. », dépenses 409,76 € | Conforme | ✅ |
| 7 | `/operations` | Modifier (2,34 €) puis supprimer l'opération du cas 6 | « Opération modifiée », 410,87 €, puis 3 opérations et 408,53 € | Conforme | ✅ |
| 8 | `/recurrences` | Ouvrir l'écran | 4 récurrences | Conforme | ✅ |
| 9 | `/reglages` | Onglets Comptes, Périodes, Catégories et libellés, Profil | Écrans chargés, console sans erreur | Conforme ; nom du profil affiché dans l'en-tête | ✅ |
| 10 | En-tête | Se déconnecter, puis ouvrir `/reglages` | `/login`, « Vous êtes déconnecté. », jeton et identifiant supprimés | Conforme (`localStorage` vide) | ✅ |
| P1 | Toute l'application | Rechargements complets entre les écrans | Session conservée (store auth relu au démarrage) | Conforme | ✅ |

## Bilan

Aucune régression constatée. L'opération de test a été supprimée.
