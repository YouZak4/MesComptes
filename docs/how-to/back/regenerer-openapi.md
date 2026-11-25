---
derniere_relecture: 2026-11-25
---

# Régénérer `openapi.json`

`docs/reference/api/openapi.json` est la description exacte de l'API, générée depuis le code. Elle se régénère après **toute** modification de l'API (route, champ, validation, code de réponse) et part dans le même commit que le code.

## Prérequis

- La base PostgreSQL de développement est accessible, comme pour les autres tests.
- Le back n'a pas besoin d'être lancé.

## Commande

Depuis `back/` :

```bash
./mvnw test -Dtest=OpenApiExportTests -Dopenapi.export=true
```

Sous PowerShell, mettre les arguments `-D` entre guillemets. Sinon PowerShell coupe `-Dopenapi.export=true` au point et Maven répond « Unknown lifecycle phase ".export=true" » :

```powershell
./mvnw test "-Dtest=OpenApiExportTests" "-Dopenapi.export=true"
```

Le test `OpenApiExportTests` démarre l'application sans serveur, lit `/v3/api-docs` et écrit `docs/reference/api/openapi.json`. Sans `-Dopenapi.export=true`, ce test est ignoré, ce qui évite de réécrire le fichier à chaque `./mvnw test`.

## Vérifier le résultat

```bash
git diff docs/reference/api/openapi.json
```

Les clés sont triées et le JSON indenté : le fichier ne change que si l'API a changé. Le diff doit correspondre exactement à la modification faite.

- **Aucun diff alors que l'API a changé** : vérifier que la route porte bien `@Operation` et que le test a tourné (il est ignoré sans le paramètre).
- **Un diff sur des routes non touchées** : une modification précédente a oublié de régénérer le fichier. Le signaler dans la PR.
