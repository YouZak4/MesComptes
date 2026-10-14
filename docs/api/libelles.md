# Libellés

Le libellé dit ce qu'est une opération (« Netflix », « Loyer ») et porte une catégorie par défaut : choisir « Netflix » remplit « Abonnements & loisirs ». Comme les catégories, un libellé est **fourni** (commun à tous, non modifiable, [masquable](masquage.md)) ou **personnel** (modifiable, archivable).

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/libelles` | Liste les libellés, ou les cherche (`?q=`) |
| `POST` | `/libelles` | Crée un libellé personnel |
| `PUT` | `/libelles/{id}` | Modifie un libellé personnel (ou le désarchive) |
| `DELETE` | `/libelles/{id}` | Retire un libellé personnel |

## Le libellé (`DtoLibelle`)

```json
{ "id": 12, "nom": "Netflix", "categorieId": 4, "fourni": true, "masque": false, "archive": false }
```

`categorieId` est la catégorie proposée à la saisie quand on choisit ce libellé.

## `GET /libelles`

| Paramètre | Sens |
|---|---|
| `q` (facultatif) | Texte tapé dans la saisie |

- **Sans `q`** : tous les libellés, fournis et personnels, masqués et archivés compris, triés par nom.
- **Avec `q`**, pour l'autocomplétion :
  - les libellés dont le nom contient `q`, sans tenir compte des majuscules ni des espaces en trop ;
  - les plus utilisés par l'utilisateur d'abord, **20 au plus** ;
  - sont exclus les libellés archivés ou masqués, et ceux dont la catégorie est archivée ou masquée.

**200** : liste de `DtoLibelle`.

## `POST /libelles`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Oui | 100 caractères au plus ; unique comme pour les catégories (majuscules et espaces ignorés) |
| `categorieId` | id | Oui | Catégorie par défaut, non archivée |
| `archive` | booléen | Non | Ignoré à la création ; voir la modification |

```json
{ "nom": "Croquettes", "categorieId": 25 }
```

- **201** : `DtoLibelle` créé.
- **409** : nom déjà pris.
- **422** : catégorie archivée.

## `PUT /libelles/{id}`

Mêmes champs que la création, tous renvoyés. `archive` est facultatif : `true` archive, `false` désarchive, et absent, l'état actuel est gardé. Garder sa catégorie archivée est permis ; passer à une catégorie archivée ne l'est pas.

- **200** : `DtoLibelle` modifié.
- **409** : nom déjà pris.
- **422** : libellé fourni, ou nouvelle catégorie archivée.

## `DELETE /libelles/{id}`

- **204** : libellé supprimé (il n'avait jamais servi).
- **200** : libellé archivé (déjà utilisé par une opération ou une récurrence).
- **422** : libellé fourni.
