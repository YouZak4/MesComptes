# Front MesComptes (Vue 3)

Conventions de code, tests et dépendances : sections « Conventions du front » de `CONTRIBUTING.md` (importé par le `CLAUDE.md` racine). Lancement et configuration : `front/README.md`.

## Commandes (depuis `front/`, avec pnpm, pas npm)

```bash
pnpm install
pnpm dev        # serveur Vite (port VITE_FRONT_PORT du .env, 5173 par défaut)
pnpm build      # vue-tsc (vérification des types) + vite build
pnpm lint       # ESLint + Prettier
pnpm test       # tests Vitest (une passe)
pnpm test:watch # tests Vitest relancés à chaque modification
```

Contrôles : `pnpm test`, `pnpm build` et `pnpm lint`.

## Configuration

- `.env` et `.env.*` sont ignorés par Git ; modèle : `.env.example`.
- L'URL de l'API est écrite en dur dans `src/services/api.ts` (`http://localhost:8085/api`) ; `VITE_BACK_URL` n'est pas utilisé.
- Alias `@` → `src/`.

## Architecture (`src/`)

Décrite dans `docs/reference/front/architecture-front.md`, importé ici.

@../docs/reference/front/architecture-front.md
