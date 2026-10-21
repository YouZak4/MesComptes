# Documentation de MesComptes

La documentation est écrite en Markdown et versionnée avec le code : quand une évolution change une règle métier ou un choix technique, la doc est mise à jour dans le même commit.

| Dossier | Contenu |
|---|---|
| `conception/` | Conception fonctionnelle et technique : périmètre, règles métier, modèle de données. |
| `adr/` | Décisions d'architecture (Architecture Decision Records), un fichier par décision. |
| `api/` | Documentation de l'API REST : une page par ressource et la description OpenAPI générée (`openapi.json`). |
| `visuel/` | Charte graphique « Sapin » : couleurs, typographie, composants et variables CSS, en Markdown et en page HTML d'exemple. |

Les schémas s'écrivent en [Mermaid](https://mermaid.js.org/), directement dans le Markdown : GitHub les affiche.

Ne jamais mettre de mot de passe, de clé ou de secret dans la documentation.
