# Utilisateur

Profil de l'utilisateur connecté. Seul l'utilisateur connecté est exposé, jamais la liste des autres.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/utilisateurs/moi` | Renvoie l'utilisateur connecté |
| `PUT` | `/utilisateurs/moi` | Modifie le nom et le prénom |
| `PUT` | `/utilisateurs/moi/mot-de-passe` | Change le mot de passe |
| `GET` | `/utilisateurs/moi/email` | Renvoie le changement d'adresse en cours |
| `PUT` | `/utilisateurs/moi/email` | Demande un changement d'adresse (code envoyé à la nouvelle) |
| `POST` | `/utilisateurs/moi/email/verification` | Valide le code et change l'adresse |
| `POST` | `/utilisateurs/moi/email/renvoi` | Renvoie un nouveau code |
| `DELETE` | `/utilisateurs/moi/email` | Annule le changement en cours |

## L'utilisateur (`DtoUtilisateur`)

```json
{
  "id": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean@exemple.fr",
  "roles": ["ROLE_UTILISATEUR"]
}
```

`prenom` vaut `null` s'il n'est pas renseigné : le nom seul est alors affiché. Ni le mot de passe ni l'identifiant de connexion ne sont renvoyés.

## `GET /utilisateurs/moi`

**200** : `DtoUtilisateur`.

## `PUT /utilisateurs/moi`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Oui | 100 caractères au plus ; deux utilisateurs peuvent avoir le même |
| `prenom` | texte | Non | 100 caractères au plus ; vide ou blanc = retiré |

**200** : `DtoUtilisateur` modifié. Le JWT reste valable.

## `PUT /utilisateurs/moi/mot-de-passe`

```json
{ "motDePasseActuel": "…", "nouveauMotDePasse": "…" }
```

- **204** : mot de passe changé.
- **422** : mot de passe actuel incorrect (422 et non 401, pour ne pas déconnecter).

## Changement d'adresse mail

La nouvelle adresse ne remplace l'ancienne qu'une fois saisi le code envoyé à la nouvelle adresse. Il y a au plus un changement en cours par utilisateur. Un changement non finalisé est supprimé au bout de 24 heures.

### Le changement en cours (`DtoChangementEmail`)

```json
{ "nouvelEmail": "nouvelle@exemple.fr", "secondesAvantRenvoi": 42 }
```

L'adresse est renvoyée en entier : c'est l'utilisateur qui vient de la saisir.

### `GET /utilisateurs/moi/email`

- **200** : `DtoChangementEmail`.
- **204** : aucun changement en cours.

### `PUT /utilisateurs/moi/email`

```json
{ "nouvelEmail": "nouvelle@exemple.fr", "motDePasse": "…" }
```

Le mot de passe protège une session restée ouverte. Le code envoyé expire au bout de 15 minutes. Une nouvelle demande remplace la précédente, mais pas avant la fin du délai de renvoi.

- **202** : `DtoChangementEmail`.
- **422** : mot de passe incorrect, ou adresse identique à l'actuelle.
- **429** : moins de 60 secondes depuis le dernier envoi.

**Adresse déjà liée à un autre compte** : la réponse est identique. Son titulaire reçoit une alerte au lieu du code, et la vérification échouera.

### `POST /utilisateurs/moi/email/verification`

```json
{ "code": "123456" }
```

- **200** : `DtoUtilisateur` avec la nouvelle adresse. L'ancienne adresse reçoit un mail qui signale le changement.
- **400** : code incorrect (tentative comptée), aucun changement en cours, code expiré ou plus de 5 tentatives. Dans les deux derniers cas, le changement est supprimé.
- **409** : adresse prise entre-temps par un autre compte.

### `POST /utilisateurs/moi/email/renvoi`

Sans corps. L'ancien code ne marche plus et le compteur de tentatives repart à zéro.

- **202** : `DtoChangementEmail`.
- **400** : aucun changement en cours.
- **429** : moins de 60 secondes depuis le dernier envoi.

### `DELETE /utilisateurs/moi/email`

**204** : changement annulé, l'adresse actuelle reste. Sans changement en cours, ne fait rien.
