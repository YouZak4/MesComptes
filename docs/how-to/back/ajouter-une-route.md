---
derniere_relecture: 2026-11-25
---

# Ajouter une route à l'API

Ce guide montre comment ajouter une route au back, du DTO jusqu'à la documentation. L'exemple est `PeriodeController`, qui suit toutes les conventions du projet.

Une route est terminée quand elle a ses trois tests, sa page dans `docs/reference/api/` et un `openapi.json` régénéré.

## 1. Écrire les DTO

Dans `model/dto/` :

- `DtoXxxSaisie` pour ce que la route reçoit, avec ses règles de validation et des messages en français :

  ```java
  @Size(max = 50, message = "Le nom de la période ne doit pas dépasser 50 caractères.")
  private String nom;
  ```

- `DtoXxx` pour ce qu'elle renvoie. Une entité ne sort jamais d'un contrôleur.

## 2. Écrire la méthode du service

Dans le service de la ressource (`service/`) :

- `@Transactional`, avec `readOnly = true` pour une lecture.
- Un nom en français (`lister`, `creer`, `modifier`, `supprimer`, `trouver`) et une Javadoc en français.
- Le premier paramètre est l'id de l'utilisateur connecté, et **chaque recherche filtre par lui** :

  ```java
  return periodeRepository.findByIdAndUtilisateurId(id, utilisateurId)
          .orElseThrow(() -> new ResourceNotFoundException(PERIODE_INTROUVABLE));
  ```

  Une ressource d'un autre utilisateur doit répondre 404, comme une ressource inexistante ([ADR 0002](../../adr/0002-404-pour-les-donnees-d-un-autre-utilisateur.md)).
- Une règle métier non respectée lève `BusinessRuleException` (422) ; un nom déjà pris, `AlreadyExist` (409).

## 3. Écrire la route dans le contrôleur

Le contrôleur reste fin : il récupère l'utilisateur, valide l'entrée et délègue au service. Chaque route porte `@Operation`, et `@ApiResponses` pour les codes autres que 200, afin d'apparaître correctement dans Swagger UI et `openapi.json`.

```java
@Operation(summary = "Crée une période", description = "Corps vide : la période suivante, d'un mois.")
@ApiResponses({
        @ApiResponse(responseCode = "201", description = "Période créée"),
        @ApiResponse(responseCode = "422", description = "Dates incohérentes ou trou avec la période précédente")
})
@PostMapping
public ResponseEntity<DtoPeriode> creer(@AuthenticationPrincipal CustomUserDetails utilisateur,
                                        @Valid @RequestBody DtoPeriodeSaisie dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(periodeService.creer(utilisateur.getId(), dto));
}
```

## 4. Écrire les trois tests

Dans `src/test/java/.../controller/`, dans la classe de tests de la ressource, qui hérite de `ApiTestBase`. Chaque route est testée sur trois cas :

| Cas | Ce qu'on vérifie |
|---|---|
| Nominal | Le statut (200, 201, 204) et le contenu de la réponse |
| Validation | Une entrée invalide répond 400 `VALIDATION_ERROR` |
| Données d'un autre | Une ressource de l'utilisateur `autre` répond 404 |

Exemple du troisième cas, tiré de `CompteEtPeriodeControllerTests` :

```java
Compte compteAutre = compte(autre, "Secret");

mvc.perform(en(moi, put("/comptes/" + compteAutre.getId())).content("""
                {"nom": "Piraté", "soldeInitial": 0, "dateSoldeInitial": "2026-01-01"}"""))
        .andExpect(status().isNotFound());
```

Puis lancer `./mvnw test` depuis `back/` (PostgreSQL doit être accessible).

## 5. Documenter la route

Dans le même commit que le code :

1. Ajouter la route à la page de sa ressource dans [`docs/reference/api/`](../../reference/api/README.md) : méthode, route, champs, règles, exemple et codes de réponse. Une nouvelle ressource a sa propre page, ajoutée au tableau du `README.md` de l'API.
2. [Régénérer `openapi.json`](regenerer-openapi.md).
3. Si la route change une règle métier, mettre à jour la [conception](../../explanation/conception-mescomptes.md).
