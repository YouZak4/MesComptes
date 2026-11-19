# Front MesComptes (Vue 3)

## Commandes (depuis `front/`, avec pnpm, pas npm)

```bash
pnpm install
pnpm dev        # serveur Vite (port VITE_FRONT_PORT du .env, 5173 par défaut)
pnpm build      # vue-tsc (vérification des types) + vite build
pnpm lint       # ESLint + Prettier
```

Il n'y a pas de tests front : `pnpm build` et `pnpm lint` sont les contrôles.

## Configuration

- `.env` et `.env.*` sont ignorés par Git ; modèle : `.env.example`.
- L'URL de l'API est écrite en dur dans `src/services/api.ts` (`http://localhost:8085/api`) ; `VITE_BACK_URL` n'est pas utilisé.
- Alias `@` → `src/`.

## Architecture (`src/`)

| Dossier | Rôle |
|---|---|
| `views/` | Écrans routés, nommés `xxxView.vue` (camelCase) |
| `components/` | Composants nommés `XxxComponent.vue` ; sous-dossiers `reglages/`, `assistant/` |
| `services/` | Appels HTTP : `api.ts` (instance axios, JWT, gestion du 401), `comptesService.ts`, `utilisateurService.ts` |
| `stores/` | Stores Pinia en syntaxe setup (`defineStore("nom", () => { … })`) |
| `composables/` | `useReferentiels` : comptes, catégories, libellés partagés |
| `types/` | Types TypeScript alignés sur les DTO du back |
| `utils/` | Formatage (`format.ts`), messages d'erreur (`erreurs.ts`), etc. |
| `styles/` | `theme.css` : variables CSS de la charte « Sapin » |

## Conventions

- `<script setup lang="ts">`, Composition API uniquement.
- Les composants n'appellent jamais axios directement : ils passent par les services.
- Erreurs d'API : afficher `messageErreur(e, "message par défaut")` (`utils/erreurs.ts`) ; le back renvoie déjà un message français affichable.
- Un 401 déconnecte l'utilisateur (intercepteur de `api.ts`) : c'est pour ça que le back répond 422 pour un mot de passe actuel faux.
- Icônes : `@lucide/vue` uniquement.
- Style : utiliser les variables de `styles/theme.css` (`--vert-700`, `--rayon-m`…), jamais de couleur en dur. Tout nouveau composant suit `docs/visuel/charte-graphique.md`.
- Prettier : 4 espaces, guillemets doubles, point-virgule, 80 colonnes, fins de ligne LF.
- Textes de l'interface en français, avec les guillemets « ».
