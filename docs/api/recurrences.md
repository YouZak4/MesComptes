# Récurrences

Une récurrence est un modèle d'opération (loyer, salaire, Netflix, prêt…) que l'application ajoute d'elle-même au bon jour : chaque nuit, et à la connexion pour rattraper les jours où le serveur était arrêté. Chaque opération générée porte l'id de sa récurrence (`recurrenceId`).

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/recurrences` | Liste les récurrences, actives et arrêtées |
| `POST` | `/recurrences` | Crée une récurrence |
| `PUT` | `/recurrences/{id}` | Modifie une récurrence (occurrences futures seulement) |
| `DELETE` | `/recurrences/{id}` | Arrête une récurrence |

Une opération existante devient aussi une récurrence avec [`POST /operations/{id}/repeter`](operations.md#post-operationsidrepeter).

## La récurrence (`DtoRecurrence`)

```json
{
  "id": 4,
  "compteId": 3,
  "type": "DEPENSE",
  "montant": 650,
  "categorieId": 1,
  "libelleId": 1,
  "note": null,
  "frequence": "MENSUELLE",
  "jour": 5,
  "dateDebut": "2026-09-25",
  "dateFin": null,
  "prochaineOccurrence": "2026-11-05",
  "active": true
}
```

| Champ | Sens |
|---|---|
| `jour` | Jour du mois, de 1 à 31. Un jour absent du mois (31 en avril) devient le dernier jour du mois. |
| `frequence` | `MENSUELLE`, ou `ANNUELLE` : une fois par an, dans le mois de `dateDebut`. |
| `dateFin` | Facultative (fin d'un prêt). |
| `prochaineOccurrence` | Prochaine date où une opération sera générée. |
| `active` | `false` quand la prochaine occurrence tomberait après la date de fin (récurrence arrêtée, prêt remboursé). |

## `GET /recurrences`

**200** : liste de `DtoRecurrence`, actives et arrêtées, triée par jour du mois.

## `POST /recurrences`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `compteId` | id | Oui | Compte non archivé |
| `montant` | montant | Oui | Positif |
| `categorieId` | id | Voir règle | Vide = catégorie par défaut du libellé ; l'un des deux est obligatoire |
| `libelleId` | id | Non | |
| `note` | texte | Non | 255 caractères au plus |
| `frequence` | `MENSUELLE` ou `ANNUELLE` | Oui | |
| `jour` | entier | Non | 1 à 31 ; vide = jour de `dateDebut` |
| `dateDebut` | date | Oui | Peut être dans le passé |
| `dateFin` | date | Non | Postérieure ou égale à `dateDebut` |

```json
{ "compteId": 3, "montant": 650, "libelleId": 1, "frequence": "MENSUELLE", "jour": 5, "dateDebut": "2026-09-25" }
```

Les occurrences déjà passées depuis `dateDebut` sont générées tout de suite, comme des opérations.

- **201** : `DtoRecurrence` créée.
- **422** : dates incohérentes ; compte, catégorie ou libellé archivé ; ni catégorie ni libellé.

## `PUT /recurrences/{id}`

Mêmes champs que la création. Seules les occurrences futures changent : les opérations déjà générées restent telles quelles.

- Si le calendrier change (fréquence, jour, dates), les occurrences reprennent à partir de demain, sans rattraper les dates passées.
- Une récurrence arrêtée repart si on lui donne une date de fin vide ou future, là encore à partir de demain.

**200** : `DtoRecurrence` modifiée.

## `DELETE /recurrences/{id}`

- **200** : récurrence **arrêtée** : sa date de fin passe à aujourd'hui (si elle n'en avait pas, ou une plus lointaine). Plus aucune occurrence après aujourd'hui ; les opérations déjà générées restent.
- **204** : récurrence **supprimée**, parce qu'elle n'avait encore rien généré.
