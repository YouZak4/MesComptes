# Périodes

Une période est un « mois de compte » aux dates réglables, par exemple d'un jour de paie au suivant. Les périodes d'un utilisateur se suivent **sans trou ni chevauchement** : chaque jour appartient à exactement une période. Une opération n'enregistre pas sa période, elle est déduite de sa date.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/periodes` | Liste les périodes |
| `POST` | `/periodes` | Crée une période |
| `PUT` | `/periodes/{id}` | Modifie une période (et la limite de sa voisine) |

Il n'y a pas de suppression.

## La période (`DtoPeriode`)

```json
{ "id": 7, "nom": "Octobre 2026", "dateDebut": "2026-09-25", "dateFin": "2026-10-25" }
```

Le nom par défaut est le mois de la date de fin, avec une majuscule.

## `GET /periodes`

**200** : liste de `DtoPeriode`, de la plus ancienne à la plus récente.

## `POST /periodes`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Non | 50 caractères au plus ; vide = nom par défaut |
| `dateDebut` | date | Voir ci-dessous | |
| `dateFin` | date | Non | Vide = un mois après le début |

Cas acceptés :

| Situation | Corps | Période créée |
|---|---|---|
| Aucune période | `dateDebut` obligatoire | Du `dateDebut` au `dateFin` (ou un mois) |
| Période suivante | `{}`, ou `dateDebut` = lendemain de la dernière | Du lendemain de la dernière, pour un mois (ou jusqu'à `dateFin`) |
| Période avant la première | `dateFin` = veille de la première, avec `dateDebut` | Du `dateDebut` au `dateFin` |

- **201** : `DtoPeriode` créée.
- **422** :
  - pas de date de début pour une première période ;
  - date de fin avant la date de début ;
  - période qui laisserait un trou. Le message donne les dates possibles.

Saisir une [opération](operations.md) datée après la dernière période crée d'elle-même les périodes manquantes, d'un mois chacune.

## `PUT /periodes/{id}`

| Champ | Type | Obligatoire | Règle |
|---|---|---|---|
| `nom` | texte | Non | 50 caractères au plus ; vide = nom par défaut (qui suit la nouvelle date de fin) |
| `dateDebut` | date | Oui | |
| `dateFin` | date | Oui | Postérieure ou égale au début |

Déplacer une limite déplace celle de la période voisine : avancer la fin d'octobre au 23 fait commencer novembre le 24. La voisine doit garder au moins un jour. Les opérations changent de période d'elles-mêmes, puisque la période se déduit de la date.

- **200** : `DtoPeriode` modifiée.
- **422** : date manquante, date de fin avant la date de début, ou voisine qui perdrait tous ses jours.
