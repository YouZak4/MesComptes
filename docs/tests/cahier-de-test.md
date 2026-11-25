# Cahier de test

Tests fonctionnels et de non-régression faits avant de pousser une branche (étape 3 de [« Avant de pousser une branche »](../../CONTRIBUTING.md#avant-de-pousser-une-branche)). Les tests automatisés (`./mvnw test`, `pnpm test`) ne sont pas repris ici : seuls leurs totaux figurent dans la synthèse de chaque campagne.

Chaque campagne a son propre fichier dans `campagnes/`, nommé `AAAA-MM-JJ-<n°>-<branche>.md`. Pour en ajouter une, copier le [modèle](campagnes/_modele.md) puis ajouter une ligne en tête du tableau ci-dessous.

Résultat : ✅ conforme, ❌ non conforme, ⏳ à faire.

## Campagnes

Campagne la plus récente en premier.

| Date | N° | Branche | Bilan |
|---|---|---|---|
| 2026-11-24 | 5 | [`chore/versions-majeures-front`](campagnes/2026-11-24-5-chore-versions-majeures-front.md) | Aucune régression constatée. |
| 2026-11-23 | 4 | [`fix/longueur-mot-de-passe`](campagnes/2026-11-23-4-fix-longueur-mot-de-passe.md) | Aucune régression constatée. |
| 2026-11-23 | 3 | [`fix/validation-champs`](campagnes/2026-11-23-3-fix-validation-champs.md) | Aucune régression constatée. |
| 2026-11-23 | 2 | [`fix/securite-dependances-front`](campagnes/2026-11-23-2-fix-securite-dependances-front.md) | Aucune régression constatée. |
| 2026-11-23 | 1 | [`test/vitest-front` et `chore/dependances-front`](campagnes/2026-11-23-1-test-vitest-front.md) | Aucune régression constatée. |
