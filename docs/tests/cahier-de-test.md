# Cahier de test

Tests fonctionnels et de non-régression faits avant de pousser une branche (voir `CLAUDE.md`, « Avant de pousser une branche »). Les tests automatisés (`./mvnw test`, `pnpm test`) ne sont pas repris ici : seuls leurs totaux figurent dans la synthèse de chaque campagne.

Résultat : ✅ conforme, ❌ non conforme, ⏳ à faire. Campagne la plus récente en premier.

## Campagne du 2026-11-23 (2) : `fix/securite-dependances-front`

Changements testés : axios 1.13.6 → 1.21.1, vue 3.5.30 → 3.5.46, vue-router 5.0.4 → 5.5.1, vite 8.0.3 → 8.3.8, `@vitejs/plugin-vue` 6.0.5 → 6.0.11, outils (eslint, prettier, vue-tsc, parsers) en version mineure. Mise en forme Prettier 3.10 de `ReglagesProfilComponent.vue` (rendu identique). Risque d'effet de bord : tous les écrans (Vue, routeur) et tous les appels à l'API (axios).

Environnement : back sur `http://localhost:8085/api` (PostgreSQL 16), front `pnpm dev` redémarré sur `http://localhost:5555` (Vue 3.5.46 servi, vérifié dans le navigateur), Chrome.

### Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 92 tests, 0 échec, 1 ignoré (export OpenAPI) |
| `pnpm test` | ✅ 84 tests |
| `pnpm lint` | ✅ après mise en forme Prettier 3.10 |
| `pnpm build` | ✅ |
| `pnpm peers check` | ✅ aucun problème |
| `pnpm audit` | ✅ 68 alertes → 2 moyennes (`glob`, `minimatch` via `@vue/test-utils` → `js-beautify`, tests uniquement) |

### Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| 1 | `/login` | Se déconnecter, puis ouvrir `/login` | Écran de connexion, aucune erreur en console | Conforme | ✅ |
| 2 | `/operations`, `/reglages` | Ouvrir l'URL sans session | Redirection vers `/login` | Conforme | ✅ |
| 3 | `/login` | Identifiant inexistant, mot de passe au bon format mais faux, Connexion | Notification « Identifiant ou mot de passe incorrect. », pas de jeton, on reste sur `/login` | Conforme | ✅ |
| 4 | `/login` → `/` | Se connecter avec un compte valide | Tableau de bord de la période courante | Novembre 2026 affiché, chiffres identiques à la campagne précédente | ✅ |
| 5 | `/` | Période précédente, puis « Période en cours » | Totaux de la période choisie | Octobre 2026 puis Novembre 2026 | ✅ |
| 6 | Saisie rapide | Touche N, 1,23 € en « Autres dépenses », note de test, Enregistrer | « Opération enregistrée. », totaux mis à jour | Dépenses 408,53 € → 409,76 € | ✅ |
| 7 | `/operations` | Modifier l'opération du cas 6 (2,34 €), puis la supprimer | « Opération modifiée. », puis « Opération supprimée. », dépenses revenues à leur montant initial | 410,87 €, puis 408,53 € | ✅ |
| 8 | `/recurrences` | Ouvrir l'écran | Liste des récurrences avec leur calendrier | 4 récurrences affichées | ✅ |
| 9 | `/reglages` | Onglets Comptes, Périodes, Catégories et libellés, Profil | Écrans chargés, aucune erreur en console | Conforme | ✅ |
| 10 | En-tête | Se déconnecter, puis ouvrir `/operations` | Retour à `/login`, jeton et identifiant supprimés | Conforme | ✅ |

### Bilan

Aucune régression constatée. L'opération créée au cas 6 a été supprimée au cas 7.

Anomalie observée, antérieure à cette branche (code non modifié) : dans `InputComponent.vue`, `onInput` émet `valid` à partir de `errorMessage`, calculé avant la mise à jour de la valeur et tant que le champ n'a pas perdu le focus. Le formulaire de connexion est donc considéré valide pendant la saisie, même si le mot de passe ne respecte pas le format : Entrée envoie la requête, alors qu'un clic sur « Connexion » (qui fait perdre le focus) affiche l'erreur de format et désactive le bouton. Sans conséquence de sécurité (le back vérifie les identifiants), mais à corriger dans une branche dédiée.

## Campagne du 2026-11-23 (1) : `test/vitest-front` et `chore/dependances-front`

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
