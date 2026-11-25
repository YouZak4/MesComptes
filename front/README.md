# Front MesComptes

Application web de MesComptes : Vue 3, TypeScript, Vite, Pinia. Elle appelle l'API du back sur `http://localhost:8085/api`.

## Prérequis

- Node 26 (version fixée dans `.nvmrc`)
- pnpm (version fixée par le champ `packageManager` de `package.json`). **Ne pas utiliser npm** : il ignorerait `pnpm-lock.yaml` et créerait un `package-lock.json`.
- Le back lancé, pour que l'application ait des données (voir [`back/README.md`](../back/README.md))

## Configuration

`.env` n'est pas versionné. Créez-le à partir du modèle :

```bash
cp .env.example .env
```

`VITE_FRONT_PORT` règle le port du serveur de développement (5173 si absent). L'URL de l'API est écrite dans `src/services/api.ts`.

## Lancer

Depuis `front/` :

```bash
pnpm install
pnpm dev
```

L'application est servie sur le port de `VITE_FRONT_PORT`.

## Commandes

| Commande | Rôle |
|---|---|
| `pnpm dev` | Serveur de développement avec rechargement à chaud |
| `pnpm build` | Vérification des types (`vue-tsc`) puis build de production dans `dist/` |
| `pnpm lint` | ESLint et Prettier |
| `pnpm test` | Tests Vitest, une passe |
| `pnpm test:watch` | Tests Vitest relancés à chaque modification |

Avant de proposer une modification, `pnpm test`, `pnpm build` et `pnpm lint` doivent passer.

## Aller plus loin

- [Architecture du front](../docs/reference/front/architecture-front.md) : dossiers de `src/` et appels à l'API
- [Charte graphique « Sapin »](../docs/reference/visuel/charte-graphique.md) : couleurs, composants et variables CSS
- [API REST](../docs/reference/api/README.md) : les routes appelées par le front
