# Campagne du 2026-11-23 (3) : `fix/validation-champs`

Légende et autres campagnes : [cahier de test](../cahier-de-test.md).

Changements testés : la validité d'`InputComponent` suit chaque saisie (un formulaire ne part plus avec un champ au mauvais format par la touche Entrée) ; la connexion n'applique plus de règle de format ; règles de l'identifiant et du mot de passe partagées (`utils/validation.ts`, `ReglesSaisie.java`) et imposées par le back à l'inscription et au changement de mot de passe ; changement de mot de passe des Réglages contrôlé côté front. Risque d'effet de bord : les trois écrans qui utilisent `InputComponent` (connexion, inscription, vérification du code) et le changement de mot de passe.

Environnement : back et front redémarrés sur la branche, `http://localhost:8085/api` et `http://localhost:5555` (Vue 3.5.30 et axios 1.13.6 de `develop`, vérifiés dans le navigateur), Chrome.

## Contrôles automatiques

| Contrôle | Résultat |
|---|---|
| `./mvnw test` (back) | ✅ 97 tests, 0 échec, 1 ignoré (export OpenAPI) ; nouveaux : `InscriptionTests` (4), règle du nouveau mot de passe (1) |
| `pnpm test` | ✅ 113 tests ; nouveaux : `InputComponent` (13), `validation` (16). Les tests d'`InputComponent` sur la validité échouent sur l'ancien composant (5 sur 10), dont le cas du bug |
| `pnpm lint`, `pnpm build` | ✅ |
| `docs/api/openapi.json` | ✅ régénéré (`pattern` sur l'identifiant et les mots de passe) |

## Cas fonctionnels

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

## Bilan

Aucune régression constatée. L'opération de test a été supprimée et le mot de passe du compte n'a pas changé.

Anomalie observée pendant la campagne puis corrigée dans la branche (cas V8 et V9) : quand un champ perdait le focus à cause d'un clic ailleurs, son message d'erreur apparaissait pendant le clic et décalait les éléments en dessous, si bien que le clic tombait à côté. Le message attend désormais la fin du clic ; au clavier, il s'affiche tout de suite.
