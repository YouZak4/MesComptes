## Ce que change cette PR

<!-- En une ou deux phrases : quoi, et pourquoi. -->

## Vérifications

Les étapes détaillées sont dans `CONTRIBUTING.md`, section « Avant de pousser une branche ».

- [ ] `./mvnw test` (back) ; `pnpm test`, `pnpm build` et `pnpm lint` (front) passent.
- [ ] Le diff a été relu en entier (`git diff develop...`).
- [ ] Campagne de test fonctionnel ajoutée dans `docs/tests/campagnes/` et dans le cahier de test.
- [ ] Revue de sécurité faite : isolation par `utilisateurId`, validation des entrées, pas de secret.
- [ ] Les pages de `docs/` touchées sont à jour, exemples compris, avec leur date de relecture.
- [ ] `openapi.json` est régénéré si l'API a changé.
- [ ] Une décision structurante a son ADR dans `docs/adr/`.
