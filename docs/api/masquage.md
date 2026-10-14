# Masquage

Un utilisateur peut cacher une catégorie ou un libellé **fourni** qu'il n'utilise pas, puis le réafficher. Un élément personnel ne se masque pas : il s'archive (voir [Catégories](categories.md) et [Libellés](libelles.md)).

| Méthode | Route | Rôle |
|---|---|---|
| `PUT` | `/masquages/{type}/{id}` | Masque un élément fourni |
| `DELETE` | `/masquages/{type}/{id}` | Réaffiche un élément fourni |

| Paramètre | Valeurs |
|---|---|
| `type` | `categorie` ou `libelle` (majuscules acceptées) |
| `id` | Id de la catégorie ou du libellé |

Un élément masqué n'est plus proposé à la saisie ni à l'autocomplétion. Les listes (`GET /categories`, `GET /libelles`) le renvoient toujours, avec `masquee` ou `masque` à `true`.

## `PUT /masquages/{type}/{id}`

Sans corps. Masquer un élément déjà masqué ne fait rien.

- **204** : élément masqué.
- **404** : type inconnu, ou élément inexistant.
- **422** : élément personnel.

## `DELETE /masquages/{type}/{id}`

Sans corps. Réafficher un élément qui n'était pas masqué ne fait rien.

- **204** : élément réaffiché.
- **404** : type inconnu, ou élément inexistant.
- **422** : élément personnel.
