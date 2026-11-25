# 0007 – Code, documentation et commits en français

- **Date :** 2026-11-19
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation. La règle est appliquée depuis la V1 (octobre 2026) et a été écrite le 2026-11-19.

## Contexte

Le domaine de MesComptes est français et ses notions n'ont pas d'équivalent exact en anglais : libellé, période de salaire, CAF, compte courant. Les premiers commits du dépôt (mars 2026) étaient en anglais ; le code de la V1 est en français.

## Décision

Tout est en français : classes, méthodes et variables, commentaires, messages d'erreur, documentation et messages de commit. Les nouveaux noms suivent ce choix (`creer`, `lister`, `supprimer`). Seuls restent en anglais les mots imposés par les outils et frameworks (`controller`, `service`, `Dto`, types de commit `feat`, `fix`…).

## Conséquences

- Le code parle la même langue que la conception et que l'utilisateur : `Libelle`, `Periode`, `Recurrence`.
- Les messages d'erreur du back s'affichent tels quels dans le front.
- Le mélange de vocabulaire avec les frameworks est accepté (`CompteService.lister`).
- Un contributeur non francophone aurait plus de mal à entrer dans le projet.
