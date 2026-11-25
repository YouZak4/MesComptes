# Documentation de MesComptes

La documentation est écrite en Markdown et versionnée avec le code : quand une évolution change une règle métier, une route ou un choix technique, la doc est mise à jour dans le même commit.

Elle est rangée selon le **besoin du lecteur** (cadre [Diátaxis](https://diataxis.fr/)), puis par couche (`back/`, `front/`) quand un document ne concerne qu'une seule partie.

| Dossier | Le lecteur veut… | Contenu |
|---|---|---|
| `how-to/` | faire une tâche précise | [Ajouter une route](how-to/back/ajouter-une-route.md), [écrire une migration Flyway](how-to/back/ecrire-une-migration-flyway.md), [régénérer `openapi.json`](how-to/back/regenerer-openapi.md) |
| `reference/` | consulter un fait exact | [API REST](reference/api/README.md) et `openapi.json`, [objets du back](reference/back/objets-back.md), [architecture du front](reference/front/architecture-front.md), [charte graphique « Sapin »](reference/visuel/charte-graphique.md) |
| `explanation/` | comprendre le pourquoi | [Document de conception](explanation/conception-mescomptes.md) : périmètre V1 et règles métier |
| [`adr/`](adr/README.md) | savoir pourquoi un choix a été fait | Décisions d'architecture, une par fichier |
| [`tests/`](tests/cahier-de-test.md) | savoir ce qui a été testé | Cahier de test : campagnes fonctionnelles et de non-régression |

Pour installer le projet, voir le [README](../README.md) ; pour contribuer, [`CONTRIBUTING.md`](../CONTRIBUTING.md).

## Règles

- Un document ne sert qu'un seul besoin : une marche à suivre ne réexplique pas la conception, elle y renvoie.
- Une information vit à un seul endroit ; les autres documents y renvoient par un lien.
- Les schémas s'écrivent en [Mermaid](https://mermaid.js.org/), directement dans le Markdown : GitHub les affiche.
- Ne jamais mettre de mot de passe, de clé ou de secret dans la documentation.
