# 0003 – Schéma géré par Hibernate, Flyway pour le reste

- **Date :** 2026-10-12
- **Statut :** Remplacé par [0008](0008-toute-modification-de-schema-par-flyway.md)
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation. Le contexte et les inconvénients marqués « à confirmer » sont déduits.

## Contexte

Le schéma de la base PostgreSQL évolue souvent pendant le développement de la V1. Écrire une migration Flyway pour chaque colonne ajoutée ralentirait ce développement (à confirmer). Mais Hibernate seul ne sait ni renommer une colonne, ni changer un type, ni reprendre des données : le passage de l'id utilisateur de `bigint` à UUID ([0004](0004-id-utilisateur-en-uuid.md)) en a eu besoin.

## Décision

- Hibernate est en `spring.jpa.hibernate.ddl-auto=update` : il crée et complète les tables à partir des entités.
- Flyway (`db/migration/V<n>__description.sql`) ne sert qu'à ce qu'Hibernate ne sait pas faire : renommages, changements de type, reprises de données.
- Flyway passe avant Hibernate. Une migration doit donc fonctionner sur une base neuve où la table n'existe pas encore, grâce à un garde (`IF NOT EXISTS … RETURN`, voir `V2__utilisateur_id_uuid.sql`).

## Conséquences

- Ajouter un champ à une entité suffit : pas de migration à écrire.
- Une migration commitée ne se modifie jamais : on en ajoute une nouvelle.
- `ddl-auto=update` ne supprime jamais une colonne ni une contrainte devenue inutile : elles restent en base. (à confirmer : faut-il un nettoyage par Flyway ?)
- Avant une mise en production, cette décision est à réévaluer : beaucoup d'équipes passent à `ddl-auto=validate` avec un schéma entièrement versionné par Flyway. (à confirmer)
