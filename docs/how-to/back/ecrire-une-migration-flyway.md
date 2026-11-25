---
derniere_relecture: 2026-11-25
---

# Écrire une migration Flyway

Dans MesComptes, Hibernate crée et complète les tables seul (`ddl-auto=update`). Une migration Flyway ne sert que pour ce qu'Hibernate ne sait pas faire ([ADR 0003](../../adr/0003-schema-par-hibernate-et-flyway.md)).

## Faut-il une migration ?

| Changement | Migration ? |
|---|---|
| Nouvelle entité, nouveau champ, nouvel index | Non : Hibernate s'en charge |
| Renommer une colonne ou une table | Oui |
| Changer le type d'une colonne (`bigint` → `uuid`) | Oui |
| Modifier des données existantes (nouvelles icônes des catégories fournies) | Oui |
| Supprimer une colonne devenue inutile | Oui : Hibernate ne supprime jamais rien |

## 1. Créer le fichier

Dans `back/src/main/resources/db/migration/`, avec le numéro suivant et une description en français, en minuscules et séparée par des `_` :

```text
V1__nom_et_prenom.sql
V2__utilisateur_id_uuid.sql
V3__icones_lucide.sql
V4__description_de_la_modification.sql   ← la nouvelle
```

Commencer le fichier par un commentaire qui dit **pourquoi** la migration existe.

## 2. Ajouter le garde pour une base neuve

Flyway passe **avant** Hibernate. Sur une base neuve, la table visée n'existe pas encore : la migration doit alors ne rien faire. Hibernate créera directement la table dans sa forme finale.

Le modèle à suivre est `V3__icones_lucide.sql` :

```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = current_schema() AND table_name = 'categorie') THEN
        RETURN;
    END IF;

    -- La modification elle-même
    UPDATE categorie SET ...;
END $$;
```

Pour une colonne, le garde teste `information_schema.columns` (voir `V2__utilisateur_id_uuid.sql`). Le garde rend aussi la migration sans effet sur une base déjà migrée.

## 3. Tester sur les deux types de base

- **Base existante** : lancer le back (`./mvnw spring-boot:run`) sur la base de développement et vérifier le résultat en base.
- **Base neuve** : créer une base vide, pointer `application.properties` dessus et relancer. Le démarrage doit réussir sans erreur Flyway.

Puis lancer `./mvnw test`.

## Règle à ne jamais enfreindre

Une migration commitée ne se modifie plus : Flyway enregistre la somme de contrôle de chaque fichier joué et refuse de démarrer si elle change. Pour corriger une migration, on en écrit une nouvelle.
