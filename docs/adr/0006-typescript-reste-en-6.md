# 0006 – Le front reste sur TypeScript 6

- **Date :** 2026-11-24
- **Statut :** Accepté
- **Note :** Rédigé après coup le 2026-11-25, à partir du code, de l'historique Git et de la documentation. Voir la [campagne de test du 2026-11-24](../tests/campagnes/2026-11-24-5-chore-versions-majeures-front.md).

## Contexte

Lors de la montée de versions du front (Pinia 4, Node 26), TypeScript 7.0.2 a été essayé. Son compilateur est réécrit en Go et ne fournit pas l'API JavaScript dont dépendent deux outils du projet :

- `vue-tsc`, utilisé par `pnpm build` pour vérifier les types des fichiers `.vue` ;
- `typescript-eslint`, utilisé par `pnpm lint`.

Avec TypeScript 7, `pnpm build` et `pnpm lint` échouent.

## Décision

Le front reste en TypeScript 6.x (6.0.3 au moment de la décision), version exacte dans `package.json`.

## Conséquences

- `pnpm build` et `pnpm lint` fonctionnent.
- Le projet ne profite pas des gains de vitesse de TypeScript 7.
- À réévaluer quand `vue-tsc` et `typescript-eslint` prendront TypeScript 7 en charge. Ce nouvel examen donnera lieu à un ADR qui remplace celui-ci.
