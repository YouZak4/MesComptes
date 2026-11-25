# Campagne du 2026-11-23 (4) : `fix/longueur-mot-de-passe`

Légende et autres campagnes : [cahier de test](../cahier-de-test.md).

Changements testés : mot de passe de 8 à 64 caractères (au lieu de 30) et 72 octets UTF-8 au plus, limite de BCrypt (`@OctetsMax` côté back, `erreurMotDePasse()` côté front) ; nouvelle prop `verifier` d'`InputComponent`, utilisée par le mot de passe de l'inscription. Risque d'effet de bord : inscription, connexion, changement de mot de passe des Réglages.

Environnement : back redémarré sur la branche, `http://localhost:8085/api` et `http://localhost:5555`, Chrome.

## Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 100 tests, 0 échec, 1 ignoré (export OpenAPI) ; nouveaux : 64 caractères et 71 octets acceptés, 73 octets refusés, connexion avec 400 octets → 401, 64 caractères acceptés au changement de mot de passe. Sans `@OctetsMax`, l'inscription à 73 octets répond 500 (`password cannot be more than 72 bytes`) |
| `pnpm test` | ✅ 117 tests ; `validation` réécrit autour d'`erreurMotDePasse`, `InputComponent` : contrôle libre (1) |
| `pnpm lint`, `pnpm build` | ✅ |
| `docs/api/openapi.json` | ✅ régénéré (`pattern` passé à `{8,64}`) |

## Cas fonctionnels

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

## Bilan

Aucune régression constatée. Le mot de passe du compte n'a pas changé ; aucun compte créé.

Faux départ pendant la campagne, sans lien avec le code final : le navigateur avait gardé en cache une version intermédiaire de `ReglagesProfilComponent.vue` (entre deux enregistrements), ce qui bloquait L2. Corrigé en forçant Vite à resservir le module.
