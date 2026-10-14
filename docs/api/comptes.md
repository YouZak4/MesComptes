# Comptes

Les comptes de l'utilisateur (« Compte courant », « Livret A »…). Un premier compte « Compte courant » est créé à l'inscription.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/comptes` | Liste les comptes, archivés compris, avec leur solde |
| `POST` | `/comptes` | Crée un compte |
| `PUT` | `/comptes/{id}` | Modifie un compte (ou le désarchive) |
| `DELETE` | `/comptes/{id}` | Retire un compte |

## Le compte (`DtoCompte`)

```json
{
  "id": 3,
  "nom": "Compte courant",
  "soldeInitial": 250.0,
  "dateSoldeInitial": "2026-09-25",
  "archive": false,
  "solde": 135.5,
  "dernierUtilise": true
}
```

| Champ | Sens |
|---|---|
| `solde` | Solde initial + revenus − dépenses datés **à partir de** `dateSoldeInitial`. Le solde initial est celui du matin de cette date : les opérations du jour même comptent. |
| `dernierUtilise` | Compte de la dernière opération saisie : à proposer par défaut à la saisie. |
| `archive` | N'est plus proposé à la saisie ; l'historique reste. |

## `GET /comptes`

**200** : liste de `DtoCompte`, archivés compris, triée par nom.

## `POST /comptes`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Oui | 100 caractères au plus |
| `soldeInitial` | montant | Oui | Peut être négatif (découvert) ; 2 décimales au plus |
| `dateSoldeInitial` | date | Oui | |
| `archive` | booléen | Non | Ignoré à la création ; voir la modification |

```json
{ "nom": "Livret A", "soldeInitial": 1200, "dateSoldeInitial": "2026-10-01" }
```

**201** : `DtoCompte` créé.

## `PUT /comptes/{id}`

Mêmes champs que la création, tous renvoyés. `archive` est facultatif : `true` archive le compte, `false` le désarchive, et absent, l'état actuel est gardé.

**200** : `DtoCompte` modifié, avec son solde recalculé.

## `DELETE /comptes/{id}`

- **204** : compte supprimé (il n'avait aucune opération).
- **200** : compte archivé (il a des opérations), renvoyé avec `"archive": true`.
