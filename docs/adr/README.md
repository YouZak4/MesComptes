# Décisions d'architecture (ADR)

Un ADR note une décision structurante et surtout **pourquoi** elle a été prise, pour ne pas avoir à refaire le débat plus tard.

## Règles

- Un fichier par décision, numéroté : `0001-titre-court.md`, `0002-...`.
- On ne modifie pas un ADR accepté. Si la décision change, on écrit un nouvel ADR qui le remplace, et on passe l'ancien au statut « Remplacé par 000X ».
- Modèle à copier : [`0000-modele.md`](0000-modele.md).

## Liste

| N° | Décision | Statut |
|---|---|---|
| 0001 | [La période d'une opération se déduit de sa date](0001-periode-deduite-de-la-date.md) | Accepté |
| 0002 | [Les données d'un autre utilisateur répondent 404, jamais 403](0002-404-pour-les-donnees-d-un-autre-utilisateur.md) | Accepté |
| 0003 | [Schéma géré par Hibernate, Flyway pour le reste](0003-schema-par-hibernate-et-flyway.md) | Remplacé par 0008 |
| 0004 | [L'id des utilisateurs est un UUID](0004-id-utilisateur-en-uuid.md) | Accepté |
| 0005 | [Un mot de passe actuel faux répond 422, pas 401](0005-mot-de-passe-actuel-faux-en-422.md) | Accepté |
| 0006 | [Le front reste sur TypeScript 6](0006-typescript-reste-en-6.md) | Accepté |
| 0007 | [Code, documentation et commits en français](0007-code-et-documentation-en-francais.md) | Accepté |
| 0008 | [Toute modification de schéma passe par Flyway](0008-toute-modification-de-schema-par-flyway.md) | Accepté |
