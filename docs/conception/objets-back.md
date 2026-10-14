# Les objets du back MesComptes

Analyse en lecture seule de `back/src/main/java/com/mescomptes/backend/model/` (état local du 13/10/2026).

## Vue d'ensemble

Le back sépare deux familles d'objets :

- **Les entités** (`model/entities`) sont les tables de la base PostgreSQL. Hibernate les crée et les met à jour (`ddl-auto=update`). Elles ne sortent jamais telles quelles de l'API.
- **Les DTO** (`model/dto`) sont ce qui circule en JSON entre le front et le back. Il y a deux sortes de DTO :
  - les DTO de **lecture** (`DtoCompte`, `DtoOperation`…), renvoyés par l'API ;
  - les DTO de **saisie** (`DtoCompteSaisie`, `DtoOperationSaisie`…), reçus de l'API, avec leurs règles de validation (`@NotNull`, `@Size`…) et leurs messages d'erreur en français.

La conversion entité vers DTO est écrite à la main dans chaque service, par une méthode qui fait `new DtoXxx(...)` (par exemple `CompteService.java:147`, `OperationService.java:133`).

Le schéma des liens :

```
Utilisateur ──< Compte ──< Operation >── Categorie
     │                        │   └──── Libelle ──> Categorie (par défaut)
     ├──< Periode             └──── Recurrence (si générée)
     ├──< Recurrence ──> Compte, Categorie, Libelle
     ├──< Categorie (perso) / Libelle (perso)
     ├──< Masquage (masque une catégorie ou un libellé fourni)
     └──<> Role
InscriptionEnAttente (isolée, avant la création de l'Utilisateur)
```

## 1. Les entités

### Utilisateur (`utilisateurs`)
L'utilisateur connecté. Son `id` est un UUID généré par l'application, pour qu'un id exposé par l'API ne révèle ni le nombre d'inscrits ni leur ordre. Il a un `nom` obligatoire (qui peut servir de pseudo) et un `prenom` facultatif, affichés tels quels, un `identifiant` (pour se connecter) et un `email`, tous deux uniques, et un `motDePasse` haché. Ses rôles sont reliés par une table de jointure `utilisateur_roles` (relation plusieurs à plusieurs avec `Role`).

Toutes les autres données (comptes, périodes, catégories perso…) pointent vers lui, ce qui permet de limiter chaque requête à ses propres données.

### Role (`role`)
Un simple nom de rôle unique (ex. `ROLE_USER`). Il sert à Spring Security : `CustomUserDetails` transforme les rôles de l'utilisateur en autorisations.

### InscriptionEnAttente (`inscriptions_en_attente`)
Une inscription pas encore confirmée par le code reçu par mail. Elle garde tout ce qu'il faut pour créer l'utilisateur ensuite (nom, prénom, identifiant, email, mot de passe **déjà haché**) plus la mécanique de vérification :

- `jeton` : identifiant opaque envoyé au front à la place de l'email ;
- `codeHash` : le code de vérification, haché, jamais stocké en clair ;
- `expiration`, `tentatives`, `dernierEnvoi` : limitent la durée de validité, le nombre d'essais (5) et la fréquence des renvois (60 s) ;
- `compteExistant` : si l'email est déjà utilisé, le back fait comme si l'inscription se passait normalement mais envoie un mail d'alerte au lieu d'un code. Ça empêche de deviner quels emails sont inscrits.

Un traitement planifié (`NettoyageInscriptionService`) supprime toutes les heures les inscriptions trop vieilles.

### Compte (`compte`)
Un compte bancaire de l'utilisateur. Il a un `nom`, un `soldeInitial` (qui peut être négatif) et une `dateSoldeInitial`. Seules les opérations datées **après** cette date comptent dans le solde : on part du solde réel à une date donnée sans avoir à saisir tout l'historique.

`archive` : un compte archivé n'est plus proposé à la saisie mais son historique reste consultable.

### Periode (`periode`)
Un cycle de budget (en général un mois) **commun à tous les comptes** de l'utilisateur, avec `dateDebut`, `dateFin` et un `nom` (par défaut le mois de la date de fin, par exemple « Février 2026 », modifiable).

Points importants :
- les opérations ne pointent **pas** vers une période : une opération appartient à la période qui contient sa date ;
- deux périodes d'un même utilisateur ne peuvent pas commencer le même jour, et la base vérifie que `date_fin >= date_debut` ;
- l'enchaînement des périodes (la fin d'une période est la veille du début de la suivante) est garanti par `PeriodeService`, pas par la base.

### Categorie (`categorie`)
Le classement d'une opération (Courses, Salaire…). Elle a un `type` (`DEPENSE` ou `REVENU`), une `icone` et une `couleur` (#RRGGBB).

Il y en a deux sortes :
- **fournie** (`utilisateur` vide) : fait partie de la liste par défaut, est commune à tous, n'est pas modifiable mais peut être masquée ;
- **personnelle** (`utilisateur` renseigné) : créée par l'utilisateur, modifiable. Si elle est déjà utilisée, elle est archivée plutôt que supprimée.

Unicité du nom : avant chaque enregistrement, `nom` est nettoyé (espaces en trop retirés) et `nomNormalise` est calculé en minuscules. La contrainte `(utilisateur_id, nom_normalise)` empêche d'avoir « Courses » et « courses  » chez le même utilisateur. C'est la classe utilitaire `Noms` qui fait ce nettoyage.

### Libelle (`libelle`)
Le « quoi » d'une opération (Loyer, Netflix, Salle de sport…). Il fonctionne comme la catégorie (fourni ou personnel, nom normalisé et unique, archivage) et a en plus une `categorieParDefaut` : quand on choisit ce libellé à la saisie, sa catégorie est proposée automatiquement, et on peut la changer sur l'opération.

### Masquage (`masquage`)
Permet à un utilisateur de cacher une catégorie ou un libellé **fourni** qu'il n'utilise pas. `typeElement` (`CATEGORIE` ou `LIBELLE`) dit de quoi il s'agit et `elementId` donne son id. Comme cet id peut pointer vers deux tables différentes, il n'y a pas de clé étrangère : la cohérence est gérée par `MasquageService`.

### Operation (`operation`)
Le cœur de l'application : une dépense ou un revenu.

- `compte` : sur quel compte ; l'utilisateur propriétaire se retrouve à travers le compte ;
- `date` : détermine aussi la période ;
- `montant` : **toujours positif** (la base le vérifie), arrondi au centime ;
- `type` : `DEPENSE` ou `REVENU`, **déduit de la catégorie**, c'est lui qui donne le sens du montant ;
- `categorie` (obligatoire), `libelle` (facultatif), `note` (texte libre) ;
- `recurrence` : renseignée si l'opération a été créée automatiquement par une récurrence.

Un index sur `(compte_id, date)` accélère les listes et les calculs de solde. La contrainte unique `(recurrence_id, date)` empêche qu'une récurrence crée deux fois la même opération le même jour.

### Recurrence (`recurrence`)
Un modèle d'opération qui revient tout seul : loyer, salaire, abonnements. Elle reprend les champs d'une opération (compte, type, montant, catégorie, libellé, note) et y ajoute un calendrier :

- `frequence` : `MENSUELLE` ou `ANNUELLE` ;
- `jour` : de 1 à 31. Si le jour n'existe pas dans le mois (31 avril), c'est le dernier jour du mois qui est utilisé ;
- `dateDebut` et `dateFin` (facultative, par exemple la fin d'un prêt ; elle est aussi remplie quand on arrête la récurrence) ;
- `prochaineOccurrence` : un **curseur**. Chaque nuit à 0h05, `GenerateurRecurrences` crée les opérations dont la date est arrivée puis fait avancer ce curseur. Comme le curseur ne revient jamais en arrière, une opération générée que tu supprimes ou dont tu changes la date n'est pas recréée.

### Les enums
- `TypeOperation` : `DEPENSE`, `REVENU`.
- `Frequence` : `MENSUELLE`, `ANNUELLE`.
- `TypeElement` : `CATEGORIE`, `LIBELLE` (pour le masquage).

### Classes d'appui
- `Noms` : nettoie les noms (`nettoyer`), les met en forme pour l'unicité (`normaliser`) et prépare un texte de recherche sûr pour un `LIKE` (`motifRecherche`).
- `CustomUserDetails` : enveloppe un `Utilisateur` pour Spring Security. Elle expose son identifiant de connexion, son mot de passe haché, ses rôles et son `id`, que les contrôleurs utilisent pour ne servir que ses données.

## 2. Les DTO

### Authentification et utilisateur
| DTO | Sens | Rôle |
|---|---|---|
| `DtoRegister` | reçu | Inscription : nom, prénom (facultatif), identifiant, mot de passe, email (email valide). |
| `DtoInscriptionEnAttente` | renvoyé | Réponse à l'inscription : le `jeton`, l'email **masqué** (jamais l'adresse complète) et `secondesAvantRenvoi` pour afficher le décompte, même après un rafraîchissement. |
| `DtoVerification` | reçu | Le `jeton` et le `code` reçu par mail pour valider l'inscription. |
| `DtoRenvoiCode` | reçu | Le `jeton` pour redemander un code. |
| `DtoLogin` | reçu | Identifiant et mot de passe pour la connexion. |
| `DtoUtilisateur` | renvoyé | L'utilisateur connecté (id UUID, nom, prénom, email, rôles), renvoyé après vérification et par « moi ». |
| `DtoApiError` | renvoyé | Format unique de toutes les erreurs : date, code HTTP, type d'erreur, message, chemin appelé. Construit par `GlobalExceptionHandler`. |

Le parcours : `DtoRegister` → `DtoInscriptionEnAttente` → (mail) → `DtoVerification` → `DtoUtilisateur`, puis `DtoLogin` pour se connecter et obtenir le jeton JWT.

### Comptes et périodes
- `DtoCompteSaisie` : nom (100 caractères max), solde initial (2 décimales max), date du solde initial. `archive` est ignoré à la création et sert à désarchiver.
- `DtoCompte` : reprend l'entité et ajoute deux valeurs calculées : `solde` (solde initial + revenus − dépenses après la date du solde initial) et `dernierUtilise` (le compte de la dernière opération saisie, proposé par défaut au front).
- `DtoPeriodeSaisie` : tout est facultatif. Sans nom, le nom par défaut est utilisé. Sans dates, `PeriodeService` les déduit des périodes existantes (la nouvelle période suit la précédente).
- `DtoPeriode` : id, nom, dates.

### Catégories et libellés
- `DtoCategorieSaisie` : nom (50 max), type obligatoire, icône (16 max), couleur au format #RRGGBB, `archive` pour désarchiver.
- `DtoCategorie` : ajoute `fournie` (liste par défaut, non modifiable) et `masquee` (cachée par l'utilisateur via un `Masquage`). Le front sait ainsi quoi afficher et quoi rendre modifiable.
- `DtoLibelleSaisie` : nom (100 max), catégorie par défaut obligatoire, `archive`.
- `DtoLibelle` : ajoute `categorieId` (catégorie proposée), `fourni` et `masque`.

### Opérations
- `DtoOperationSaisie` : compte et montant obligatoires, montant strictement positif. Pas de champ `type` : il vient de la catégorie. Raccourcis : date vide = aujourd'hui, catégorie vide = catégorie par défaut du libellé (il faut l'un des deux). Note limitée à 255 caractères.
- `DtoOperation` : l'opération avec des **ids** à la place des objets liés (`compteId`, `categorieId`, `libelleId`, `recurrenceId`). Le front retrouve les noms dans les listes qu'il a déjà chargées.
- `FiltreOperations` : un `record` (objet immuable) qui regroupe les filtres facultatifs et combinables de `GET /operations` : période, compte, catégorie, libellé et un texte `q` cherché dans la note et le libellé. Il est traduit en requête par `OperationSpecifications`.

### Récurrences
- `DtoRecurrenceSaisie` : les champs d'une opération + fréquence (obligatoire), jour (1 à 31 ; vide = jour de la date de début), date de début (obligatoire) et date de fin (facultative). Le type vient là aussi de la catégorie.
- `DtoRecurrence` : l'entité, plus `active` (faux une fois la date de fin passée) et `prochaineOccurrence`.
- `DtoOccurrence` : une opération **pas encore générée** qu'une récurrence va créer à une date donnée. Elle n'existe pas en base : elle est calculée pour le tableau de bord.

### Tableau de bord
- `DtoTableauDeBord` : tout ce qu'affiche l'écran d'une période, pour un compte ou pour tous (`compteId` vide) :
  - `periode`, avec `periodePrecedenteId` et `periodeSuivanteId` pour naviguer ;
  - `revenus`, `depenses`, `solde` : l'équivalent des GAINS, PERTES et BENEFICES de ton Excel ;
  - `parCategorie` : liste de `DtoTotalCategorie`, de la plus grosse à la plus petite ;
  - `aVenir` : les `DtoOccurrence` des récurrences restant à passer dans la période ;
  - `soldePrevu` : le solde de la période une fois ces occurrences passées ;
  - `soldeComptesFinPeriode` : le solde du ou des comptes en fin de période, occurrences à venir comprises ;
  - `dernieresOperations`.
- `DtoTotalCategorie` : le total d'une catégorie sur la période (avec son nom, son icône et sa couleur pour l'affichage), son total sur la période précédente (zéro s'il n'y en a pas) et l'`ecart` entre les deux.

## 3. Points que j'ai remarqués

Ce ne sont pas des bugs constatés, seulement des choses à garder en tête.

- **`Utilisateur` et `Role` utilisent `@Data` de Lombok.** Sur une entité JPA, `@Data` génère `equals`, `hashCode` et `toString` sur tous les champs, y compris la collection `roles` chargée à la demande. Ça peut déclencher des chargements inattendus ou une erreur hors transaction. Les autres entités utilisent `@Getter`/`@Setter`, ce qui est plus sûr. `Role` a aussi des annotations en double (`@Getter` avec `@Data`, `@EqualsAndHashCode` avec `@Data`).
- **`DtoUtilisateur` expose l'entité `Role`** au lieu d'une simple liste de noms de rôles. C'est le seul DTO qui contient une entité.
- **`DtoLogin` n'a pas de validation** (`@NotBlank`), contrairement aux autres DTO de saisie. Un champ vide finira probablement en erreur d'identifiants plutôt qu'en message clair (déduit, non testé).
- **Longueurs non alignées** entre DTO et base : `DtoCategorieSaisie` limite le nom à 50 caractères, mais la colonne `nom` de l'entité n'a pas de `length` (255 par défaut). Ce n'est pas gênant, juste à savoir si un jour une autre voie que l'API écrit en base.
