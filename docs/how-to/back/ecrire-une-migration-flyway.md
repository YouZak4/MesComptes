---
derniere_relecture: 2026-11-25
---

# Écrire une migration Flyway

Dans MesComptes, **toute** modification de la base passe par une migration Flyway : nouvelle table, nouveau champ, index, renommage, reprise de données ([ADR 0008](../../adr/0008-toute-modification-de-schema-par-flyway.md)). Hibernate est en `ddl-auto=validate` : il ne touche jamais au schéma, il vérifie seulement qu'il correspond aux entités.

Une modification d'entité sans migration se voit tout de suite : l'application refuse de démarrer, en local comme en CI.

## 1. Créer le fichier

Dans `back/src/main/resources/db/migration/`, avec le numéro qui suit le plus grand existant et une description en français, en minuscules, séparée par des `_` :

```text
V1__schema_initial.sql
V2__ajout_budget_mensuel.sql   ← la nouvelle
```

Avant de merger, vérifier qu'aucune autre branche n'a pris le même numéro entre-temps.

Commencer le fichier par un commentaire qui dit **pourquoi** la migration existe.

## 2. Écrire le changement

Un sujet par migration. Exemple : ajouter un budget mensuel aux catégories.

```sql
-- Budget mensuel facultatif par catégorie (écran Réglages > Catégories).
ALTER TABLE categorie ADD COLUMN budget_mensuel NUMERIC(12, 2);

ALTER TABLE categorie
    ADD CONSTRAINT ck_categorie_budget_positif CHECK (budget_mensuel IS NULL OR budget_mensuel > 0);
```

Règles :

- **Nommer explicitement** les contraintes et les index : `pk_<table>`, `fk_<table>_<cible>`, `uk_<table>_<colonnes>`, `ck_<table>_<règle>`, `idx_<table>_<colonnes>`, comme dans `V1__schema_initial.sql`. Un nom généré est illisible et difficile à cibler plus tard.
- **Types alignés sur les entités** : `NUMERIC` pour un `BigDecimal`, `UUID` pour un `UUID`, `VARCHAR(n)` avec la même longueur que `@Column(length = n)`. Sinon, `validate` refuse de démarrer.
- **Reprise de données séparée** du changement de schéma : une migration pour ajouter la colonne, une autre pour la remplir si le remplissage est complexe.
- Pas de `CREATE INDEX CONCURRENTLY` mélangé à autre chose : PostgreSQL ne l'accepte pas dans une transaction, il doit être seul dans sa migration.

PostgreSQL exécute chaque migration dans une transaction : si une instruction échoue, toute la migration est annulée et la base reste dans son état précédent.

## 3. Mettre à jour l'entité

Dans le même commit, modifier l'entité Java pour qu'elle corresponde exactement au schéma :

```java
@Column(name = "budget_mensuel", precision = 12, scale = 2)
private BigDecimal budgetMensuel;
```

## 4. Tester

- **Sur ta base de développement** : lancer le back (`./mvnw spring-boot:run`). Flyway joue la nouvelle migration, puis Hibernate valide le schéma. Vérifier le résultat en base.
- **Sur une base neuve** : lancer `./mvnw test`. La CI le fait aussi à chaque pull request, sur une base PostgreSQL vide où toutes les migrations sont jouées depuis V1.

## Un changement de structure sur des données réelles

Une fois une PPROD ou une PROD en service, un changement qui casse l'existant (renommer ou supprimer une colonne, rendre un champ obligatoire) se fait en plusieurs versions de l'application :

1. ajouter la nouvelle colonne, nullable ;
2. recopier les données dans la nouvelle colonne ;
3. ajouter la contrainte (`NOT NULL`…) et basculer le code dessus ;
4. supprimer l'ancienne colonne dans une version ultérieure.

## Règles à ne jamais enfreindre

- **Une migration commitée ne se modifie plus.** Flyway enregistre la somme de contrôle de chaque fichier joué et refuse de démarrer si elle change. Pour corriger, on écrit une nouvelle migration.
- **On ne revient jamais en arrière** : pas de script d'annulation, une erreur se corrige par une migration de plus. Une sauvegarde de la base précède chaque déploiement en PROD.
- **Mêmes scripts partout** : aucune migration propre à un environnement.
