# 0002 – Les données d'un autre utilisateur répondent 404, jamais 403

- **Date :** 2026-10-12
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation.

## Contexte

Chaque donnée appartient à un utilisateur. Quand un utilisateur demande une ressource qui ne lui appartient pas (`GET /comptes/42` où le compte 42 est à quelqu'un d'autre), l'API peut répondre 403 (interdit) ou 404 (introuvable).

Un 403 confirme que la ressource existe : en essayant des ids, on pourrait savoir combien de comptes ou d'opérations existent, et lesquels.

## Décision

Une ressource d'un autre utilisateur est traitée exactement comme une ressource inexistante : `ResourceNotFoundException`, réponse **404** `RESOURCE_NOT_FOUND`. Les requêtes filtrent toujours par utilisateur (`findByIdAndUtilisateurId`…).

## Conséquences

- L'API ne révèle jamais l'existence des données des autres.
- Chaque nouvelle route doit être testée sur ce cas : `ApiTestBase` fournit un second utilisateur, `autre`, pour vérifier le 404.
- Le 403 `ACCESS_DENIED` reste réservé à un rôle manquant (aucune route ne l'exige pour l'instant).
- Un filtre oublié dans une requête ne lèverait pas d'erreur visible : la revue de sécurité avant chaque push vérifie l'isolation par `utilisateurId`.
