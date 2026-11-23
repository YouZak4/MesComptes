# Cahier de test

Tests fonctionnels et de non-régression faits avant de pousser une branche (voir `CLAUDE.md`, « Avant de pousser une branche »). Les tests automatisés (`./mvnw test`, `pnpm test`) ne sont pas repris ici : seuls leurs totaux figurent dans la synthèse de chaque campagne.

Résultat : ✅ conforme, ❌ non conforme, ⏳ à faire.

## Campagne du 2026-11-23 : `test/vitest-front` et `chore/dependances-front`

Changements testés : mise en place de Vitest (aucun code applicatif modifié), versions figées, `pnpm-lock.yaml` versionné, plugins ESLint inutilisés retirés. Risque d'effet de bord : le build et le chargement de l'application, puisque les dépendances changent.

Environnement : back sur `http://localhost:8085/api` (PostgreSQL 16), front `pnpm dev` sur `http://localhost:5555`, branche `chore/dependances-front` (contient les deux), Chrome.

### Contrôles automatiques

| Contrôle | `test/vitest-front` | `chore/dependances-front` |
|---|---|---|
| `./mvnw test` (back) | ✅ 92 tests, 0 échec, 1 ignoré (export OpenAPI) | ✅ identique (back non modifié) |
| `pnpm test` | ✅ 84 tests | ✅ 84 tests |
| `pnpm lint` | ✅ | ✅ |
| `pnpm build` | ✅ | ✅ |
| `pnpm peers check` | ❌ dépendance pair non satisfaite (sans fichier de verrouillage, les versions varient d'une installation à l'autre) | ✅ aucun problème |
| `pnpm install --frozen-lockfile` | sans objet (pas de fichier de verrouillage) | ✅ |

### Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| 1 | `/login` | Ouvrir `http://localhost:5555/` sans session | Redirection vers l'écran de connexion, aucune erreur en console | Conforme | ✅ |
| 2 | `/operations`, `/reglages` | Ouvrir l'URL sans session | Redirection vers `/login` | Conforme | ✅ |
| 3 | `/login` | Identifiant inexistant, mauvais mot de passe, valider | Notification « Identifiant ou mot de passe incorrect. », pas de jeton enregistré, on reste sur `/login` | Conforme | ✅ |
| 4 | `/login` → `/` | Se connecter avec un compte valide | Tableau de bord de la période courante | Novembre 2026 affiché : solde, revenus, dépenses, répartition, dernières opérations, à venir | ✅ |
| 5 | `/` | Période précédente, puis « Période en cours » | Totaux, répartition et opérations de la période choisie | Octobre 2026 puis Novembre 2026, URL `?periode=…` à jour | ✅ |
| 6 | Saisie rapide | « Nouvelle opération », 1,23 € en « Autres dépenses », note de test, Enregistrer | Notification « Opération enregistrée. », totaux mis à jour | Dépenses 408,53 € → 409,76 €, opération en tête des dernières opérations | ✅ |
| 7 | `/operations` | Modifier l'opération du cas 6 (2,34 €), puis la supprimer (confirmation dans la ligne) | « Opération modifiée. », puis « Opération supprimée. », dépenses revenues à leur montant initial | Dépenses 410,87 € après modification, 408,53 € après suppression | ✅ |
| 8 | `/recurrences` | Ouvrir l'écran | Liste des récurrences avec leur calendrier | 4 récurrences, « Le 4 de chaque mois », prochaine échéance affichée | ✅ |
| 9 | `/reglages` | Onglets Comptes, Périodes, Catégories et libellés, Profil | Écrans chargés, aucune erreur en console | Conforme, console sans erreur | ✅ |
| 10 | En-tête | Se déconnecter, puis ouvrir `/operations` | Retour à `/login`, jeton et identifiant supprimés, page protégée inaccessible | Conforme | ✅ |

### Bilan

Aucune régression constatée. Le compte de test n'a gardé aucune trace : l'opération créée au cas 6 a été supprimée au cas 7.
