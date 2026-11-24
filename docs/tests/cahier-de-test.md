# Cahier de test

Tests fonctionnels et de non-régression faits avant de pousser une branche (voir `CLAUDE.md`, « Avant de pousser une branche »). Les tests automatisés (`./mvnw test`, `pnpm test`) ne sont pas repris ici : seuls leurs totaux figurent dans la synthèse de chaque campagne.

Résultat : ✅ conforme, ❌ non conforme, ⏳ à faire. Campagne la plus récente en premier.

## Campagne du 2026-11-24 (5) : `chore/versions-majeures-front`

Changements testés : Pinia 3.0.4 → 4.1.0 (avec `@vue/devtools-api` 8.2.1 installé explicitement, comme Pinia 4 l'exige), TypeScript 5.9.3 → 6.0.3, `@types/node` 25.5.0 → 26.6.6 et projet fixé sur Node 26 (`.nvmrc`, `engines`) ; `baseUrl` retiré des tsconfig (obsolète en TypeScript 6). TypeScript 7.0.2 essayé puis écarté : `pnpm build` (vue-tsc) et `pnpm lint` (typescript-eslint) échouent, faute d'API JavaScript. Risque d'effet de bord : tout ce qui passe par les stores Pinia (auth, profil, notification, saisie) et le contrôle de types.

Environnement : Node 26.13.1 (fnm) et pnpm 11.23.0, serveur front relancé sur la branche (Pinia v4.1.0 vérifié dans le cache de dépendances de Vite), back inchangé sur `develop`, Chrome.

### Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 100 tests, 0 échec, 1 ignoré (export OpenAPI) ; back non modifié |
| `pnpm test` (Node 26) | ✅ 117 tests |
| `pnpm lint`, `pnpm build` (Node 26) | ✅ |
| `tsc -p tsconfig.node.json --noEmit` | ✅ (`vite.config.ts`, hors du contrôle de `pnpm build`) |
| `pnpm peers check` | ✅ aucun conflit |
| `pnpm audit` | 2 alertes moyennes, déjà connues (js-beautify via `@vue/test-utils`, tests uniquement) |

### Cas fonctionnels

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

### Bilan

Aucune régression constatée. L'opération de test a été supprimée.

## Campagne du 2026-11-23 (4) : `fix/longueur-mot-de-passe`

Changements testés : mot de passe de 8 à 64 caractères (au lieu de 30) et 72 octets UTF-8 au plus, limite de BCrypt (`@OctetsMax` côté back, `erreurMotDePasse()` côté front) ; nouvelle prop `verifier` d'`InputComponent`, utilisée par le mot de passe de l'inscription. Risque d'effet de bord : inscription, connexion, changement de mot de passe des Réglages.

Environnement : back redémarré sur la branche, `http://localhost:8085/api` et `http://localhost:5555`, Chrome.

### Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 100 tests, 0 échec, 1 ignoré (export OpenAPI) ; nouveaux : 64 caractères et 71 octets acceptés, 73 octets refusés, connexion avec 400 octets → 401, 64 caractères acceptés au changement de mot de passe. Sans `@OctetsMax`, l'inscription à 73 octets répond 500 (`password cannot be more than 72 bytes`) |
| `pnpm test` | ✅ 117 tests ; `validation` réécrit autour d'`erreurMotDePasse`, `InputComponent` : contrôle libre (1) |
| `pnpm lint`, `pnpm build` | ✅ |
| `docs/api/openapi.json` | ✅ régénéré (`pattern` passé à `{8,64}`) |

### Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| L1 | Réglages > Profil | Lire l'aide du formulaire de mot de passe | « 8 à 64 caractères… » | Conforme | ✅ |
| L2 | Réglages > Profil | Actuel quelconque, nouveau et confirmation `A1-` + 35 « é » (38 caractères, 73 octets) | « Mot de passe trop long… », aucune requête | Conforme | ✅ |
| L3 | Réglages > Profil | Actuel volontairement faux, nouveau et confirmation de 64 caractères conformes | Requête envoyée, la longueur passe la validation : 422 « Le mot de passe actuel est incorrect. », session conservée | Conforme, mot de passe du compte inchangé | ✅ |
| L4 | API `POST /auth/register` | Appel direct avec 73 octets, puis avec 65 caractères | 400 « Le mot de passe est trop long… », puis 400 avec la règle « entre 8 et 64 caractères » ; aucun mail | Conforme | ✅ |
| L5 | API `POST /auth/login` | Identifiant inexistant, mot de passe de 403 octets | 401 `AUTH_FAILED`, pas de 500 | Conforme | ✅ |
| L6 | `/login` | Identifiant `abc`, mot de passe accentué de 43 caractères (83 octets) | Aucune règle de format à la connexion : bouton « Connexion » actif, pas de message | Conforme | ✅ |
| L7 | Inscription | Mot de passe de 73 octets puis clic dans un autre champ ; 64 caractères ; 65 caractères | « trop long » ; aucune erreur ; rappel de la règle « 8 à 64 caractères… » | Conforme (formulaire non soumis : aucun mail envoyé) | ✅ |
| 4 | `/` | Ouvrir le tableau de bord | Novembre 2026, chiffres identiques à la campagne (3) | Solde -294,51 €, dépenses 408,53 € | ✅ |
| 10 | En-tête | Se déconnecter | `/login`, « Vous êtes déconnecté. » | Conforme | ✅ |

### Bilan

Aucune régression constatée. Le mot de passe du compte n'a pas changé ; aucun compte créé.

Faux départ pendant la campagne, sans lien avec le code final : le navigateur avait gardé en cache une version intermédiaire de `ReglagesProfilComponent.vue` (entre deux enregistrements), ce qui bloquait L2. Corrigé en forçant Vite à resservir le module.

## Campagne du 2026-11-23 (3) : `fix/validation-champs`

Changements testés : la validité d'`InputComponent` suit chaque saisie (un formulaire ne part plus avec un champ au mauvais format par la touche Entrée) ; la connexion n'applique plus de règle de format ; règles de l'identifiant et du mot de passe partagées (`utils/validation.ts`, `ReglesSaisie.java`) et imposées par le back à l'inscription et au changement de mot de passe ; changement de mot de passe des Réglages contrôlé côté front. Risque d'effet de bord : les trois écrans qui utilisent `InputComponent` (connexion, inscription, vérification du code) et le changement de mot de passe.

Environnement : back et front redémarrés sur la branche, `http://localhost:8085/api` et `http://localhost:5555` (Vue 3.5.30 et axios 1.13.6 de `develop`, vérifiés dans le navigateur), Chrome.

### Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 97 tests, 0 échec, 1 ignoré (export OpenAPI) ; nouveaux : `InscriptionTests` (4), règle du nouveau mot de passe (1) |
| `pnpm test` | ✅ 113 tests ; nouveaux : `InputComponent` (13), `validation` (16). Les tests d'`InputComponent` sur la validité échouent sur l'ancien composant (5 sur 10), dont le cas du bug |
| `pnpm lint`, `pnpm build` | ✅ |
| `docs/api/openapi.json` | ✅ régénéré (`pattern` sur l'identifiant et les mots de passe) |

### Cas fonctionnels

| # | Écran / route | Étapes | Attendu | Obtenu | Résultat |
|---|---|---|---|---|---|
| V1 | `/login` | Identifiant `abc`, mot de passe `abc`, Entrée | Pas de règle de format : la requête part, notification « Identifiant ou mot de passe incorrect. » | Conforme | ✅ |
| V2 | `/login` | Vider le mot de passe | Bouton « Connexion » désactivé, « Ce champ est obligatoire. » à la sortie du champ | Conforme | ✅ |
| V3 | Inscription | Tous les champs remplis, mot de passe sans caractère spécial (confirmation identique), Entrée dans le champ e-mail | Aucune requête envoyée, bouton désactivé, erreur de format sous le mot de passe | Aucune requête (espion sur `XMLHttpRequest`), bouton désactivé | ✅ |
| V4 | Inscription | Mot de passe et confirmation conformes | Bouton « Créer mon compte » actif (formulaire non soumis : aucun mail envoyé) | Conforme | ✅ |
| V5 | API `POST /auth/register` | Appel direct avec le mot de passe `abc`, puis l'identifiant `a b` | 400 avec le message de la règle, aucun mail | 400 et messages du back pour les deux | ✅ |
| V6 | Réglages > Profil | Actuel quelconque, nouveau et confirmation `abc`, « Changer le mot de passe » | Erreur de règle affichée, aucune requête | Conforme ; rappel de la règle affiché au-dessus du formulaire | ✅ |
| V7 | Réglages > Profil | Actuel volontairement faux, nouveau conforme | Requête envoyée, « Le mot de passe actuel est incorrect. » (422), session conservée | Conforme, mot de passe du compte inchangé | ✅ |
| V8 | `/login` | Identifiant saisi, mot de passe saisi puis effacé (focus dedans), un seul clic sur « Créer un nouveau compte » | Le clic ouvre l'inscription (avant correction, le message apparu à la perte du focus décalait le lien et le clic tombait à côté) | Conforme | ✅ |
| V9 | Inscription | Quitter « Nom » vide par un clic ailleurs, puis « Identifiant » vide par Tab | Message « Ce champ est obligatoire. » affiché après le clic, et tout de suite avec Tab | Conforme | ✅ |
| 1 à 3 | `/login`, pages protégées | Voir campagne (1) | | Couverts par V1, V2 et la redirection de `/reglages` après déconnexion | ✅ |
| 4 | `/` | Se connecter | Tableau de bord | Novembre 2026, chiffres identiques aux campagnes précédentes | ✅ |
| 5 | `/` | Période précédente, puis « Période en cours » | Octobre puis Novembre 2026 | Conforme | ✅ |
| 6 | Saisie rapide | Touche N, 1,23 € en « Autres dépenses », note de test | « Opération enregistrée. », dépenses 409,76 € | Conforme | ✅ |
| 7 | `/operations` | Modifier (2,34 €) puis supprimer l'opération du cas 6 | 410,87 €, puis 408,53 € | Conforme | ✅ |
| 8 | `/recurrences` | Ouvrir l'écran | 4 récurrences | Conforme | ✅ |
| 9 | `/reglages` | Onglets Comptes, Périodes, Catégories et libellés, Profil | Écrans chargés, console sans erreur | Conforme | ✅ |
| 10 | En-tête | Se déconnecter, puis ouvrir `/reglages` | `/login`, jeton et identifiant supprimés | Conforme | ✅ |

### Bilan

Aucune régression constatée. L'opération de test a été supprimée et le mot de passe du compte n'a pas changé.

Anomalie observée pendant la campagne puis corrigée dans la branche (cas V8 et V9) : quand un champ perdait le focus à cause d'un clic ailleurs, son message d'erreur apparaissait pendant le clic et décalait les éléments en dessous, si bien que le clic tombait à côté. Le message attend désormais la fin du clic ; au clavier, il s'affiche tout de suite.

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
