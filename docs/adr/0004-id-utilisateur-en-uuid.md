# 0004 – L'id des utilisateurs est un UUID

- **Date :** 2026-10-14
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation.

## Contexte

L'id d'un utilisateur était un compteur (`bigint`). Il est exposé par l'API, notamment dans `DtoUtilisateur`. Un compteur révèle le nombre d'inscrits et leur ordre d'inscription : l'id 57 dit qu'il y a au moins 57 comptes.

## Décision

L'id d'un utilisateur est un UUID généré par l'application. La migration `V2__utilisateur_id_uuid.sql` a converti les données existantes et toutes les clés étrangères `utilisateur_id`. Les autres entités (comptes, opérations…) gardent un id entier.

## Conséquences

- Un id d'utilisateur exposé ne révèle plus rien.
- Les autres ids entiers ne posent pas ce problème, puisqu'une ressource d'un autre utilisateur répond 404 ([0002](0002-404-pour-les-donnees-d-un-autre-utilisateur.md)).
- Le front et les tests manipulent l'id utilisateur comme une chaîne, et les autres ids comme des nombres.
