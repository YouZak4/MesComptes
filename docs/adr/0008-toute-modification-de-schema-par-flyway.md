# 0008 – Toute modification de schéma passe par Flyway

- **Date :** 2026-11-25
- **Statut :** Accepté
- **Remplace :** [0003 – Schéma géré par Hibernate, Flyway pour le reste](0003-schema-par-hibernate-et-flyway.md)

## Contexte

Jusqu'ici, Hibernate créait et complétait les tables seul (`ddl-auto=update`) ; Flyway ne servait qu'à ce qu'Hibernate ne sait pas faire ([0003](0003-schema-par-hibernate-et-flyway.md)). Ce fonctionnement pose trois problèmes avant le premier déploiement :

- **Pas d'historique** : le schéma d'une base dépend de toutes les versions de l'application qui ont démarré dessus. Deux bases peuvent diverger sans que personne ne le voie.
- **Pas de déploiement maîtrisé** : en PPROD ou en PROD, c'est Hibernate qui modifierait la base au démarrage, sans script relu ni testé au préalable.
- **Des changements impossibles** : Hibernate ne renomme, ne supprime et ne reprend jamais rien. Les colonnes devenues inutiles restent en base.

Il n'existe encore ni PPROD ni PROD : seule la base de développement existe. C'est le moment le moins coûteux pour changer.

## Décision

- **Toute modification du schéma** (table, colonne, contrainte, index) et **toute reprise de données** passent par une migration Flyway versionnée : `back/src/main/resources/db/migration/V<n>__description.sql`.
- Hibernate ne modifie plus le schéma, il le **vérifie** : `spring.jpa.hibernate.ddl-auto=validate`. L'application refuse de démarrer si une entité ne correspond pas au schéma.
- Les migrations V1 à V3 existantes sont **regroupées** en un unique `V1__schema_initial.sql`, qui décrit le schéma actuel. C'est une exception unique à la règle « une migration commitée ne se modifie jamais », possible parce qu'aucune base hors développement n'a joué ces migrations. La base de développement est recréée à cette occasion.
- Les mêmes scripts sont joués, dans le même ordre, sur tous les environnements. Aucun script n'est propre à un environnement.
- Les versions sont numérotées à la suite (`V2`, `V3`…). Avec un seul développeur, le risque de collision entre branches est faible ; il se vérifie en revue. À revoir si l'équipe grandit (numérotation par horodatage).

## Conséquences

- Le schéma de chaque base est reproductible et son historique est lisible dans `flyway_schema_history`.
- Les scripts testés en développement et en CI sont exactement ceux qui seront joués en PPROD puis en PROD.
- Ajouter un champ à une entité demande désormais d'écrire sa migration dans le même commit. Un oubli fait échouer le démarrage, donc la CI, qui démarre l'application sur une base neuve.
- On ne revient jamais en arrière : une erreur se corrige par une nouvelle migration, et une sauvegarde précède chaque déploiement en PROD.
- Restent à décider avant le premier déploiement : un compte de base dédié aux migrations en PROD (l'application sans droits DDL), et le passage en migration des données de référence, aujourd'hui posées par `DonneesFourniesInitializer`.
