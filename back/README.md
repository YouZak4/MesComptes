# Back MesComptes

API REST de MesComptes : Spring Boot 4.2, Java 21, PostgreSQL. Elle sert le front sous `http://localhost:8085/api`.

## Prérequis

- Java 21 (le wrapper Maven `mvnw` est fourni, pas besoin d'installer Maven)
- PostgreSQL avec une base `mescomptes` **dont l'utilisateur de l'application est propriétaire**. Depuis PostgreSQL 15, seul le propriétaire peut créer des tables dans le schéma `public` ; sinon Flyway échoue au démarrage avec « droit refusé pour le schéma public ». Si la base est créée par `postgres` :

  ```sql
  CREATE DATABASE mescomptes OWNER "<utilisateur_appli>";
  ```

## Configuration

`src/main/resources/application.properties` n'est pas versionné. Créez-le à partir du modèle, puis remplacez les valeurs `A_REMPLACER` (connexion à la base, secret JWT) :

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

## Lancer

Depuis `back/` :

```bash
./mvnw spring-boot:run
```

- API : <http://localhost:8085/api>
- Swagger UI : <http://localhost:8085/api/swagger-ui.html>

Au démarrage, Flyway joue les migrations de `src/main/resources/db/migration/`, Hibernate vérifie que les entités correspondent au schéma, puis les catégories et libellés fournis sont insérés.

## Tester

```bash
./mvnw test
```

Les tests d'API tournent sur la vraie base PostgreSQL : elle doit être accessible. Chaque test est annulé à la fin, la base n'est pas modifiée.

Après une modification de l'API, [régénérer `openapi.json`](../docs/how-to/back/regenerer-openapi.md).

## Aller plus loin

- [API REST](../docs/reference/api/README.md) : routes, règles et codes d'erreur
- [Les objets du back](../docs/reference/back/objets-back.md) : entités et DTO
- [Document de conception](../docs/explanation/conception-mescomptes.md) : règles métier
- Guides pratiques : [ajouter une route](../docs/how-to/back/ajouter-une-route.md), [écrire une migration Flyway](../docs/how-to/back/ecrire-une-migration-flyway.md)
