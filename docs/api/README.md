# API REST de MesComptes

L'API expose 40 routes sous `/api` (port 8085 en local). Elles exigent toutes un JWT, sauf l'inscription et la connexion, et ne renvoient que les données de l'utilisateur connecté.

Deux sources décrivent l'API :

- **Ces pages**, écrites à la main : les règles métier, les cas particuliers et des exemples.
- **[`openapi.json`](openapi.json)**, généré depuis le code : la description exacte des routes et des schémas, à importer dans un client (Postman, Insomnia, Bruno) ou à parcourir dans Swagger UI.

## Pages

| Page | Routes |
|---|---|
| [Authentification](authentification.md) | Inscription avec code par mail, connexion (`/auth`) |
| [Utilisateur](utilisateur.md) | Profil, mot de passe, changement d'adresse mail (`/utilisateurs/moi`) |
| [Comptes](comptes.md) | `/comptes` |
| [Périodes](periodes.md) | `/periodes` |
| [Catégories](categories.md) | `/categories` |
| [Libellés](libelles.md) | `/libelles` |
| [Masquage](masquage.md) | `/masquages/{type}/{id}` |
| [Opérations](operations.md) | `/operations` |
| [Récurrences](recurrences.md) | `/recurrences` |
| [Tableau de bord](tableau-de-bord.md) | `/tableau-de-bord` |

## Conventions

### Authentification

1. `POST /auth/login` renvoie le jeton, en texte brut.
2. Chaque requête le passe dans l'en-tête `Authorization: Bearer <jeton>`.
3. Le jeton est valable 24 heures. Il porte l'identifiant de connexion, pas l'id de l'utilisateur.

Sans jeton valable, une route protégée répond **401** avec le corps d'erreur habituel. Le champ `error` en dit la raison : `AUTH_REQUIRED` (pas de jeton), `TOKEN_EXPIRED` (jeton expiré) ou `INVALID_TOKEN` (jeton malformé, mal signé, ou utilisateur qui n'existe plus). Sur une route publique, un jeton refusé est simplement ignoré.

### Formats

| Donnée | Format | Exemple |
|---|---|---|
| Date | ISO 8601 `AAAA-MM-JJ` | `"2026-10-14"` |
| Montant | Nombre JSON, 2 décimales au plus, en euros | `12.5` |
| Id d'utilisateur | UUID | `"3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90"` |
| Autres ids | Entier | `42` |
| Type d'opération | `DEPENSE` ou `REVENU` | |
| Fréquence | `MENSUELLE` ou `ANNUELLE` | |

Un montant est toujours positif : le type (dépense ou revenu) donne le sens, et il est déduit de la catégorie.

### Données des autres utilisateurs

Une ressource qui appartient à un autre utilisateur répond **404**, comme une ressource qui n'existe pas : l'API ne révèle pas son existence.

### Éléments fournis et personnels

Les catégories et libellés **fournis** sont communs à tous les utilisateurs : on ne peut pas les modifier, seulement les [masquer](masquage.md). Les éléments **personnels** se créent, se modifient et s'archivent.

« Retirer » un compte, une catégorie ou un libellé le **supprime** s'il n'a jamais servi, et l'**archive** sinon. La réponse le dit : **204** sans corps pour une suppression, **200** avec l'élément archivé. Un élément archivé n'est plus proposé à la saisie mais son historique reste. Dans le `PUT` d'un élément personnel, le champ `archive` est facultatif : `true` l'archive, `false` le désarchive, et absent, l'état actuel est gardé.

### Erreurs

Toutes les erreurs ont le même corps (`DtoApiError`) :

```json
{
  "timestamp": "2026-10-14T13:27:27.19",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Ce compte n'existe pas.",
  "path": "/comptes/42"
}
```

`message` est en français et peut être affiché tel quel.

| Statut | `error` | Cas |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Champ obligatoire manquant, format ou longueur. Les messages de tous les champs en faute sont mis bout à bout. |
| 400 | `MALFORMED_REQUEST` | Corps absent ou JSON illisible. |
| 400 | `INVALID_VERIFICATION_CODE` | Code de vérification incorrect ou expiré, trop de tentatives, ou aucune vérification en cours. |
| 401 | `AUTH_FAILED` | Identifiant ou mot de passe incorrect (connexion). |
| 401 | `AUTH_REQUIRED` | Pas de jeton sur une route protégée. |
| 401 | `TOKEN_EXPIRED` | Jeton expiré : il faut se reconnecter. |
| 401 | `INVALID_TOKEN` | Jeton malformé, signé avec une autre clé, ou d'un utilisateur qui n'existe plus. |
| 403 | `ACCESS_DENIED` | Utilisateur connecté sans le rôle requis (aucune route ne l'exige pour l'instant). |
| 404 | `RESOURCE_NOT_FOUND` | Ressource inexistante ou d'un autre utilisateur. |
| 409 | `RESOURCE_ALREADY_EXISTS` | Nom déjà pris, adresse ou identifiant pris entre-temps. |
| 422 | `BUSINESS_RULE_VIOLATION` | Règle métier : élément archivé ou fourni, dates incohérentes, mot de passe actuel faux… |
| 429 | `TOO_MANY_REQUESTS` | Nouveau code demandé avant la fin du délai de renvoi (60 s). |
| 500 | `SERVER_ERROR` | Erreur inattendue. |

Un mot de passe actuel faux répond **422** et non 401, pour que le front, qui déconnecte sur 401, ne ferme pas la session.

## Swagger UI et `openapi.json`

Le back décrit son API avec [springdoc](https://springdoc.org/), à partir des contrôleurs et de leurs annotations `@Operation` :

| Adresse (back lancé en local) | Contenu |
|---|---|
| http://localhost:8085/api/swagger-ui.html | Interface Swagger UI : parcourir et essayer les routes (bouton « Authorize » pour coller le JWT). |
| http://localhost:8085/api/v3/api-docs | Description OpenAPI en JSON. |
| http://localhost:8085/api/v3/api-docs.yaml | La même, en YAML. |

`springdoc.api-docs.enabled=false` dans `application.properties` coupe les deux, par exemple sur un serveur public.

### Régénérer `openapi.json`

Après une modification de l'API, depuis `back/` :

```bash
./mvnw test -Dtest=OpenApiExportTests -Dopenapi.export=true
```

Le test démarre l'application sans serveur, lit `/v3/api-docs` et écrit `docs/api/openapi.json`. Il faut que la base PostgreSQL soit accessible, comme pour les autres tests. Les clés sont triées et le JSON indenté : le fichier ne change dans Git que si l'API change. Sans `-Dopenapi.export=true`, ce test est ignoré.
