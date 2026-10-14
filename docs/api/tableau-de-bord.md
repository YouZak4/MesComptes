# Tableau de bord

Synthèse d'une période pour un compte ou tous : revenus, dépenses, totaux par catégorie comparés à la période précédente, récurrences à venir et soldes prévus.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/tableau-de-bord` | Construit le tableau de bord d'une période |

## `GET /tableau-de-bord`

| Paramètre | Sens |
|---|---|
| `periodeId` (facultatif) | Période affichée. Vide : celle qui contient aujourd'hui, sinon la dernière. |
| `compteId` (facultatif) | Compte affiché. Vide : tous les comptes, archivés compris. |

```
GET /api/tableau-de-bord?periodeId=7&compteId=3
```

- **200** : `DtoTableauDeBord`.
- **404** : période ou compte introuvable, ou d'un autre utilisateur.
- **422** : l'utilisateur n'a encore aucune période. Le front s'en sert pour lancer l'assistant de première connexion.

## Le tableau de bord (`DtoTableauDeBord`)

```json
{
  "periode": { "id": 7, "nom": "Octobre 2026", "dateDebut": "2026-09-25", "dateFin": "2026-10-25" },
  "periodePrecedenteId": 6,
  "periodeSuivanteId": null,
  "compteId": null,
  "revenus": 2100,
  "depenses": 1234.5,
  "solde": 865.5,
  "parCategorie": [
    { "categorieId": 1, "nom": "Logement", "type": "DEPENSE", "icone": "🏠", "couleur": "#8D6E63",
      "total": 650, "totalPrecedent": 650, "ecart": 0 }
  ],
  "aVenir": [
    { "recurrenceId": 9, "date": "2026-10-20", "compteId": 3, "type": "DEPENSE", "montant": 15.99,
      "categorieId": 4, "libelleId": 12, "note": null }
  ],
  "soldePrevu": 849.51,
  "soldeComptesFinPeriode": 3210.4,
  "dernieresOperations": []
}
```

| Champ | Sens |
|---|---|
| `periodePrecedenteId`, `periodeSuivanteId` | Pour naviguer d'une période à l'autre ; `null` aux extrémités. |
| `compteId` | Compte affiché ; `null` pour tous les comptes. |
| `revenus`, `depenses` | Sommes des opérations de la période. |
| `solde` | `revenus − depenses`. |
| `parCategorie` | Totaux par catégorie (`DtoTotalCategorie`), du plus gros au plus petit. Une catégorie à zéro sur la période mais présente sur la précédente apparaît aussi, pour l'écart. |
| `totalPrecedent`, `ecart` | Total de la période précédente (0 s'il n'y en a pas) et `total − totalPrecedent`. |
| `aVenir` | Occurrences de récurrences pas encore générées d'ici la fin de la période (`DtoOccurrence`). |
| `soldePrevu` | Solde de la période une fois les occurrences à venir passées. |
| `soldeComptesFinPeriode` | Solde du ou des comptes à la fin de la période, occurrences à venir comprises. |
| `dernieresOperations` | Les 10 dernières opérations de la période (`DtoOperation`, voir [Opérations](operations.md)). |
