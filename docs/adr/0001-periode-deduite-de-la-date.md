# 0001 – La période d'une opération se déduit de sa date

- **Date :** 2026-10-12
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation.

## Contexte

Une période est un « mois de compte » aux dates réglables, du jour du salaire à la veille du suivant (du 25/09 au 24/10). L'utilisateur peut déplacer ses limites à tout moment : avancer la fin d'octobre au 23 fait commencer novembre le 24.

Si chaque opération enregistrait sa période (clé étrangère `periode_id`), chaque déplacement de limite obligerait à réécrire les opérations concernées, avec un risque d'incohérence entre la date d'une opération et sa période.

## Décision

L'opération n'a pas de clé étrangère vers la période. Une opération appartient à la période qui contient sa date. Les périodes d'un utilisateur se suivent sans trou ni chevauchement, ce que garantit `PeriodeService` : chaque jour appartient à exactement une période.

## Conséquences

- Déplacer une limite reclasse automatiquement les opérations, sans aucune écriture sur `operation`.
- La contiguïté n'est pas vérifiée par la base mais par le service : toute écriture de période doit passer par `PeriodeService`.
- Les requêtes par période filtrent sur un intervalle de dates ; l'index `operation (compte_id, date)` les garde rapides.
- Une opération datée hors des périodes connues provoque la création des périodes manquantes.
