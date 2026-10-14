# Opérations

Une opération est une dépense ou un revenu sur un compte, à une date. Son type n'est pas saisi : il est déduit de sa catégorie. Sa période n'est pas enregistrée non plus : elle se déduit de sa date.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/operations` | Liste les opérations, filtrées |
| `POST` | `/operations` | Crée une opération |
| `PUT` | `/operations/{id}` | Modifie une opération |
| `POST` | `/operations/{id}/repeter` | Transforme une opération en récurrence (« Répéter chaque mois ») |
| `DELETE` | `/operations/{id}` | Supprime une opération |

## L'opération (`DtoOperation`)

```json
{
  "id": 58,
  "compteId": 3,
  "date": "2026-10-12",
  "type": "DEPENSE",
  "montant": 23.9,
  "categorieId": 2,
  "libelleId": 15,
  "note": "courses de la semaine",
  "recurrenceId": null
}
```

| Champ | Sens |
|---|---|
| `montant` | Toujours positif : `type` donne le sens |
| `libelleId`, `note` | Facultatifs (`null`) |
| `recurrenceId` | Renseigné si l'opération a été générée par une [récurrence](recurrences.md) |

## `GET /operations`

Filtres facultatifs et combinables, en paramètres de requête :

| Paramètre | Sens |
|---|---|
| `periodeId` | Opérations datées dans cette période |
| `compteId` | Opérations de ce compte |
| `categorieId` | Opérations de cette catégorie |
| `libelleId` | Opérations de ce libellé |
| `q` | Texte contenu dans la note ou le nom du libellé, sans tenir compte des majuscules |

```
GET /api/operations?periodeId=7&categorieId=2&q=courses
```

- **200** : liste de `DtoOperation`, des plus récentes aux plus anciennes (date, puis ordre de saisie).
- **404** : un filtre désigne une période, un compte, une catégorie ou un libellé introuvable, ou d'un autre utilisateur. Une catégorie ou un libellé fourni est un filtre valable.

## `POST /operations`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `compteId` | id | Oui | Compte non archivé |
| `date` | date | Non | Vide = aujourd'hui |
| `montant` | montant | Oui | Positif ; arrondi au centime, au moins 0,01 € |
| `categorieId` | id | Voir règle | Vide = catégorie par défaut du libellé ; l'un des deux est obligatoire ; non archivée |
| `libelleId` | id | Non | Non archivé |
| `note` | texte | Non | 255 caractères au plus |

```json
{ "compteId": 3, "montant": 15.99, "libelleId": 12 }
```

Ici, la catégorie est celle de « Netflix » et la date est aujourd'hui. Si la date tombe après la dernière période, les périodes manquantes sont créées, d'un mois chacune.

- **201** : `DtoOperation` créée.
- **422** : compte, catégorie ou libellé archivé ; ni catégorie ni libellé ; montant arrondi à 0.

## `PUT /operations/{id}`

Mêmes champs que la création. Modifier une opération générée par une récurrence ne change qu'elle : le mois où la facture est à 17,99 € au lieu de 20,17 €.

- **200** : `DtoOperation` modifiée.
- **422** : comme à la création, ou date déplacée sur un jour où sa récurrence a déjà une opération.

## `POST /operations/{id}/repeter`

« Répéter chaque mois » : crée une [récurrence](recurrences.md) qui commence à la date de l'opération, avec ses compte, montant, catégorie, libellé et note. L'opération en devient la première occurrence.

| Paramètre | Valeurs |
|---|---|
| `frequence` (facultatif) | `MENSUELLE` (par défaut) ou `ANNUELLE` |

- **201** : la récurrence créée (`DtoRecurrence`).
- **422** : l'opération fait déjà partie d'une récurrence.

## `DELETE /operations/{id}`

**204** : opération supprimée. Une opération générée par une récurrence n'est pas recréée.
