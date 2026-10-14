# MesComptes

Application web de suivi régulier des dépenses : plusieurs comptes par utilisateur, périodes mensuelles réglables, libellés et catégories, opérations récurrentes générées automatiquement et tableau de bord par période.

## Organisation du dépôt

| Dossier | Contenu |
|---|---|
| `back/` | API Spring Boot (Java 21, PostgreSQL, migrations Flyway) |
| `front/` | Application Vue 3 (Vite, Pinia, pnpm) |
| `docs/` | Documentation du projet |

## Lancer le projet en local

### Prérequis

- Java 21
- PostgreSQL avec une base `mescomptes`
- Node.js et pnpm

### Back

```bash
cd back
./mvnw spring-boot:run
```

L'API écoute sur le port 8085. La configuration (connexion à la base, secret JWT) se trouve dans `back/src/main/resources/application.properties`, ignoré par Git : copiez `application.properties.example` à côté sous ce nom et remplacez les valeurs `A_REMPLACER`.

### Front

```bash
cd front
pnpm install
pnpm dev
```

L'application est servie sur le port 5173 par défaut.

## Documentation

- [Document de conception](docs/conception/conception-mescomptes.md) : périmètre de la V1 et règles métier.
- [Les objets du back](docs/conception/objets-back.md) : entités, DTO et leurs liens.
- [Décisions d'architecture](docs/adr/README.md) : pourquoi les choix structurants ont été faits.
