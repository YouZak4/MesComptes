# Catégories

La catégorie sert aux totaux du tableau de bord ; son type (`DEPENSE` ou `REVENU`) donne le sens des opérations. Il y a deux sortes de catégories :

- les catégories **fournies**, communes à tous (Logement, Courses, Salaire…) : non modifiables, mais [masquables](masquage.md) ;
- les catégories **personnelles**, créées par l'utilisateur : modifiables et archivables.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/categories` | Liste les catégories fournies et personnelles |
| `POST` | `/categories` | Crée une catégorie personnelle |
| `PUT` | `/categories/{id}` | Modifie une catégorie personnelle (ou la désarchive) |
| `DELETE` | `/categories/{id}` | Retire une catégorie personnelle |

## La catégorie (`DtoCategorie`)

```json
{
  "id": 2,
  "nom": "Courses",
  "type": "DEPENSE",
  "icone": "🛒",
  "couleur": "#43A047",
  "fournie": true,
  "masquee": false,
  "archive": false
}
```

`masquee` dit si l'utilisateur a masqué cette catégorie fournie. Une catégorie personnelle ne se masque pas, elle s'archive.

## `GET /categories`

**200** : liste de `DtoCategorie`, fournies et personnelles, masquées et archivées comprises. Elle est triée par type, puis par nom.

## `POST /categories`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Oui | 50 caractères au plus |
| `type` | `DEPENSE` ou `REVENU` | Oui | |
| `icone` | texte | Non | Un emoji ; 16 caractères au plus |
| `couleur` | texte | Non | Format `#RRGGBB` |
| `archive` | booléen | Non | Ignoré à la création ; voir la modification |

Le nom doit être unique parmi les catégories fournies et personnelles visibles de l'utilisateur, sans tenir compte des majuscules ni des espaces en trop : « courses  » est refusé à cause de « Courses ».

```json
{ "nom": "Animaux", "type": "DEPENSE", "icone": "🐾", "couleur": "#8D6E63" }
```

- **201** : `DtoCategorie` créée.
- **409** : nom déjà pris.

## `PUT /categories/{id}`

Mêmes champs que la création, tous renvoyés. `archive` est facultatif : `true` archive, `false` désarchive, et absent, l'état actuel est gardé.

- **200** : `DtoCategorie` modifiée.
- **409** : nom déjà pris.
- **422** : catégorie fournie, ou changement de type d'une catégorie déjà utilisée par une opération, une récurrence ou un libellé.

## `DELETE /categories/{id}`

- **204** : catégorie supprimée (elle n'avait jamais servi).
- **200** : catégorie archivée, parce qu'elle est utilisée par une opération, une récurrence ou comme catégorie par défaut d'un libellé.
- **422** : catégorie fournie.
