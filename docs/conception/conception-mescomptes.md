# MesComptes – Document de conception

Validé par Romain le 12/10/2026. Version de référence : [Claude Doc](https://claude.ai/code/artifact/f1f39ecd-43b1-4975-83bc-d796b4316e08) (ce fichier en est une copie, sans le schéma interactif).

## Objectif et périmètre

MesComptes remplace le fichier Excel de suivi mensuel par une application web, utilisée dans un navigateur classique, où noter une dépense prend quelques secondes. L'Excel 2026 a été abandonné après mai : la saisie y était trop lourde, et près de la moitié des lignes revenaient à l'identique chaque mois (loyer, prêt voiture, abonnements, salaire, CAF).

La V1 couvre :

- plusieurs comptes par utilisateur ;
- des périodes mensuelles dont la date de début et de fin se règlent ;
- une liste de catégories et de libellés fournie, complétée par ceux de l'utilisateur ;
- la saisie des dépenses et des revenus ;
- les opérations récurrentes générées automatiquement ;
- un tableau de bord par période.

L'application est pensée multi-utilisateur dès le départ : chaque donnée appartient à un utilisateur, même si un seul l'utilise au début. Les données de l'Excel servent à définir les règles et la liste de libellés fournie ; elles ne sont pas importées.

## Utilisateurs et comptes

Un utilisateur possède un ou plusieurs comptes ; il ne voit jamais les données d'un autre utilisateur. L'inscription et la connexion existent déjà dans le dépôt (entité `Utilisateur`, JWT).

- Un **compte** a un nom (« Compte courant », « Livret A »…), un solde initial et une date de solde initial.
- Le solde d'un compte = solde initial + revenus − dépenses datés à partir de la date de solde initial. Le solde initial est celui du matin de cette date : les opérations du jour même comptent (sinon, ce qui est saisi le jour de l'inscription serait ignoré, le premier compte étant créé à la date du jour).
- Un compte peut être archivé : il disparaît de la saisie mais son historique reste consultable.
- L'utilisateur a un **nom** obligatoire et un **prénom** facultatif (un pseudo peut tenir dans le nom seul). Le mot de passe est saisi deux fois à l'inscription. Changer d'adresse mail demande le mot de passe puis un code envoyé à la nouvelle adresse.
- À l'inscription, un premier compte « Compte courant » est créé automatiquement pour que l'utilisateur puisse saisir tout de suite.
- Les virements entre deux comptes d'un même utilisateur ne sont pas gérés en V1.

## Périodes

Une période est un « mois de compte » dont l'utilisateur règle la date de début et de fin, comme dans l'Excel (Février = du 23/01 au 25/02). Les périodes sont communes à tous les comptes de l'utilisateur.

- **Contiguïté :** les périodes se suivent sans trou ni chevauchement. Modifier la date de fin d'une période déplace la date de début de la suivante, et inversement. Ainsi, chaque jour appartient à exactement une période.
- **Nom :** par défaut le mois de la date de fin (« Février 2026 » pour 23/01 → 25/02), modifiable.
- **Création :** la période suivante commence le lendemain de la fin de la précédente et dure un mois par défaut ; l'utilisateur ajuste la date de fin quand il connaît son jour de paie.
- **Création automatique :** saisir une opération datée après la dernière période, ou avant la première, crée la ou les périodes manquantes avec la durée par défaut.
- **Première période :** demandée à la première connexion (date de début et de fin), préremplie avec le mois en cours.
- **Appartenance :** une opération n'enregistre pas sa période ; elle est déduite de sa date. Déplacer une limite de période reclasse donc automatiquement les opérations concernées.
- La durée par défaut est un réglage de l'utilisateur (un mois en V1).

## Catégories et libellés

La catégorie sert aux totaux ; le libellé dit ce qu'est l'opération et porte une catégorie par défaut, ce qui rend la saisie rapide (choisir « Netflix » remplit « Abonnements & loisirs »).

**Règles**

- Une **catégorie** a un nom, un type (dépense ou revenu), une icône et une couleur.
- Un **libellé** a un nom et une catégorie par défaut. Sur une opération, la catégorie proposée reste modifiable.
- Les catégories et libellés **fournis** sont communs à tous les utilisateurs et non modifiables. Chaque utilisateur peut les masquer.
- Les catégories et libellés **personnels** ne sont visibles que par leur créateur. Un libellé personnel se crée directement depuis la saisie en tapant un nom qui n'existe pas.
- Les noms sont uniques par utilisateur, sans tenir compte des majuscules ni des espaces en trop (l'Excel contient « grande surface » et « grande surface » avec un espace final).
- Un élément personnel déjà utilisé ne se supprime pas : il s'archive, pour que l'historique garde ses libellés.
- La catégorie est obligatoire sur une opération ; le libellé est facultatif (dans l'Excel, beaucoup de lignes n'en ont pas).

**Liste fournie (proposition)** — basée sur les colonnes de l'Excel, avec Logement, Véhicule, Sport et Banque sortis de « Autres » où ils étaient mélangés.

| Catégorie | Type | Exemples de libellés fournis |
| --- | --- | --- |
| Logement | Dépense | Loyer, Électricité, Gaz, Eau, Taxe d'habitation |
| Courses | Dépense | Supermarché, Boulangerie, Marché, Boucherie |
| Sorties & restaurants | Dépense | Restaurant, Fast-food, Café, Bar |
| Abonnements & loisirs | Dépense | Netflix, Deezer, Spotify, Cinéma, Jeux vidéo |
| Téléphone & internet | Dépense | Forfait mobile, Box internet |
| Assurances | Dépense | Assurance auto, Assurance moto, Assurance habitation, Prévoyance, Assurance vie |
| Santé | Dépense | Médecin, Pharmacie, Opticien, Dentiste, Mutuelle |
| Soins & beauté | Dépense | Coiffeur, Cosmétiques |
| Carburant | Dépense | Essence voiture, Essence moto |
| Véhicule | Dépense | Entretien, Révision, Péage, Parking, Équipement moto, Permis |
| Prêts | Dépense | Prêt auto, Prêt moto, Crédit immobilier |
| Banque | Dépense | Cotisation carte, Frais bancaires |
| Sport | Dépense | Salle de sport, Compléments alimentaires, Inscription course |
| Vacances & voyages | Dépense | Train, Avion, Hôtel, Location |
| Maison & entretien | Dépense | Laverie, Bricolage, Ménage |
| Cadeaux | Dépense | Cadeau anniversaire, Cadeau Noël |
| Autres dépenses | Dépense | (aucun) |
| Salaire | Revenu | Salaire, Prime |
| Aides | Revenu | CAF, Aide au logement |
| Remboursements | Revenu | Remboursement mutuelle, Remboursement d'un proche, Covoiturage |
| Autres revenus | Revenu | (aucun) |

## Opérations

Une opération est une dépense ou un revenu sur un compte, à une date ; c'est la seule chose que l'utilisateur saisit au quotidien.

| Champ | Obligatoire | Règle |
| --- | --- | --- |
| Compte | Oui | Par défaut, le dernier compte utilisé |
| Date | Oui | Par défaut, aujourd'hui ; si l'écran affiche une autre période, son premier jour |
| Type | Oui | Dépense ou revenu ; déduit de la catégorie |
| Montant | Oui | Positif, 2 décimales, en euros ; le type donne le sens |
| Catégorie | Oui | Remplie par le libellé, modifiable |
| Libellé | Non | Choisi ou créé à la volée |
| Note | Non | Texte libre (« resto anniv Camille ») |
| Justificatifs | Non | Version suivante |

- Le montant est saisi en positif ; l'application calcule le signe. Plus besoin des montants négatifs de l'Excel.
- Les montants sont stockés en décimal exact (`BigDecimal` / `NUMERIC(12,2)`), jamais en nombre à virgule flottante. L'Excel contient par exemple −93,1375 € : l'application arrondit au centime.
- Une opération se modifie et se supprime librement en V1.
- Une opération issue d'une récurrence garde le lien vers celle-ci (voir la section suivante).

## Opérations récurrentes

Une récurrence est un modèle d'opération (loyer, salaire, Netflix, prêt voiture…) que l'application ajoute toute seule au bon jour. Elle supprime la moitié de la saisie constatée dans l'Excel.

- Une récurrence a les champs d'une opération (compte, type, montant, catégorie, libellé, note), plus une **fréquence** (mensuelle ou annuelle en V1), un **jour** (1 à 31), une **date de début** et une **date de fin** facultative (fin d'un prêt).
- Un jour qui n'existe pas dans le mois (31 en avril) devient le dernier jour du mois.
- Une tâche planifiée quotidienne crée les opérations dont la date est arrivée. Un rattrapage à la connexion couvre les jours où le serveur était arrêté.
- Chaque occurrence n'est créée qu'une fois (unicité récurrence + date), même si la tâche tourne deux fois.
- Modifier une opération générée ne change que celle-ci (un mois où la facture téléphone est à 17,99 € au lieu de 20,17 €). La supprimer ne la recrée pas.
- Modifier une récurrence ne touche que les occurrences futures.
- Le tableau de bord affiche les occurrences **à venir** de la période en cours, ce qui donne une prévision du solde de fin de période.
- Une opération saisie à la main peut être transformée en récurrence en un clic (« Répéter chaque mois »).

## Justificatifs (version suivante)

Une opération pourra porter un ou plusieurs justificatifs (ticket de caisse, facture, devis) ; la V1 ne les implémente pas, mais le modèle leur réserve déjà leur place.

- Formats acceptés : JPEG, PNG, HEIC et PDF, 10 Mo maximum par fichier.
- Les fichiers sont stockés hors de la base (disque du serveur au début, stockage objet type S3 ensuite) ; la base ne garde que les métadonnées.
- Un justificatif n'est accessible qu'au propriétaire de l'opération, via l'API authentifiée, jamais par une URL publique.
- Supprimer une opération supprime ses justificatifs.
- Plus tard : lire le montant et la date sur la photo pour préremplir l'opération.

## Écrans

Cinq écrans suffisent pour la V1 ; l'écran principal est le tableau de bord, avec la saisie accessible en un clic depuis partout.

1. **Tableau de bord de la période** — sélecteur de période (précédente / suivante) et de compte (un compte ou tous).
   - Trois chiffres : revenus, dépenses, solde de la période (l'équivalent de GAINS, PERTES, BENEFICES).
   - Total par catégorie, trié du plus gros au plus petit, avec l'écart par rapport à la période précédente (« Identique à la période précédente » s'il est nul). Une catégorie sans dépense sur la période reste listée à 0 € si elle en avait sur la précédente, pour montrer l'écart.
   - Au-delà de 6 catégories de dépenses, les plus petites sont regroupées en « Autres », qui se déplie pour montrer chacune avec son montant et sa part des dépenses.
   - Opérations récurrentes à venir et solde prévu en fin de période.
   - Liste des dernières opérations.
2. **Saisie rapide** — une fenêtre ouverte par un bouton « + » toujours visible, et par le raccourci clavier N.
   - Montant en premier, puis libellé avec autocomplétion (les plus utilisés proposés d'abord), catégorie remplie automatiquement.
   - À l'inverse, une catégorie choisie sans libellé liste ses libellés : un clic en choisit un, sans devoir retrouver son nom ni passer par les réglages. Même chose dans le formulaire des récurrences.
   - Date et compte préremplis ; Entrée valide. « Enregistrer et en ajouter une autre » pour enchaîner (la date est gardée).
   - Depuis une période passée ou future (tableau de bord, opérations), la date proposée est le premier jour de cette période.
   - Le calendrier de la date montre une période entière (du 25 juillet au 24 août…), même à cheval sur deux mois, et passe d'une période à l'autre. Hors des périodes connues, et dans les autres formulaires, il montre un mois civil.
3. **Opérations** — liste de la période avec filtres (compte, catégorie, libellé, texte), modification et suppression.
4. **Récurrences** — liste, création, modification, arrêt.
5. **Réglages** — comptes, périodes (dates de début et de fin), catégories et libellés (créer, masquer, archiver), profil.

Un **assistant de première connexion** enchaîne : première période, comptes, puis les récurrences courantes à cocher (loyer, salaire, abonnements…).

## Modèle de données

L'opération est l'entité centrale ; la période n'est pas une clé étrangère mais se retrouve par la date de l'opération, ce qui permet de déplacer ses limites sans rien réécrire.

Entités et relations (le schéma est dans le Claude Doc) :

- **Utilisateur** : possède tout ce qui suit (les éléments fournis n'ont pas d'utilisateur).
- **Compte** : nom, solde initial, date du solde, archivé.
- **Catégorie** : nom, type, icône, couleur ; fournie ou personnelle.
- **Libellé** : nom, catégorie par défaut (→ Catégorie) ; fourni ou personnel.
- **Opération** : date, type, montant, note ; → Compte, → Catégorie, → Libellé (facultatif), → Récurrence (facultatif, si générée).
- **Récurrence** : fréquence, jour, début, fin + les champs d'une opération ; génère des Opérations.
- **Période** : nom, date début, date fin ; retrouvée par la date de l'opération (pas de clé étrangère).
- **Justificatif** (version suivante) : fichier, format, taille ; 0 à n par Opération.

**Contraintes en base (PostgreSQL)**

- `utilisateur_id` sur `compte`, `periode`, `operation` (via le compte) et `recurrence` ; nullable sur `categorie` et `libelle`, où vide signifie « fourni ».
- Table `masquage (utilisateur_id, type_element, element_id)` pour cacher un élément fourni.
- Unicité du nom par utilisateur sur `categorie` et `libelle`, calculée sur `lower(trim(nom))`.
- `montant NUMERIC(12,2) CHECK (montant > 0)` sur `operation` et `recurrence`.
- Unicité `(recurrence_id, date)` sur `operation`, pour ne jamais générer deux fois la même occurrence.
- Contiguïté des périodes vérifiée par le service : la fin d'une période est la veille du début de la suivante.
- Index sur `operation (compte_id, date)` pour le tableau de bord.

## API REST

Toutes les routes sont sous `/api`, protégées par le JWT existant, et ne renvoient que les données de l'utilisateur connecté ; une ressource d'un autre utilisateur répond 404.

| Ressource | Routes | Remarques |
| --- | --- | --- |
| Comptes | `GET/POST /comptes`, `PUT/DELETE /comptes/{id}` | DELETE archive le compte s'il a des opérations |
| Périodes | `GET/POST /periodes`, `PUT /periodes/{id}` | PUT déplace aussi la limite de la période voisine |
| Catégories | `GET/POST /categories`, `PUT/DELETE /categories/{id}` | GET renvoie fournies + personnelles, avec l'indicateur masqué |
| Libellés | `GET/POST /libelles`, `PUT/DELETE /libelles/{id}` | `GET /libelles?q=net` pour l'autocomplétion, trié par usage |
| Masquage | `PUT/DELETE /masquages/{type}/{id}` | Masque ou réaffiche un élément fourni |
| Opérations | `GET/POST /operations`, `PUT/DELETE /operations/{id}` | Filtres : `periodeId`, `compteId`, `categorieId`, `libelleId`, `q` |
| Récurrences | `GET/POST /recurrences`, `PUT/DELETE /recurrences/{id}` | DELETE arrête la récurrence, l'historique reste |
| Tableau de bord | `GET /tableau-de-bord?periodeId=&compteId=` | Totaux, par catégorie, écarts, à venir, solde prévu |
| Profil | `GET/PUT /utilisateurs/moi`, `PUT /utilisateurs/moi/mot-de-passe` | PUT change le nom et le prénom ; le mot de passe exige l'actuel (422 s'il est faux, pas 401 qui déconnecterait) |
| Adresse mail | `GET/PUT/DELETE /utilisateurs/moi/email`, `POST /utilisateurs/moi/email/verification`, `POST /utilisateurs/moi/email/renvoi` | PUT exige le mot de passe et envoie un code à la nouvelle adresse, qui ne remplace l'ancienne qu'après `verification` ; l'ancienne adresse est prévenue |

## Hors périmètre et suite

La V1 s'arrête à la saisie, aux récurrences et au tableau de bord ; le reste vient ensuite, dans cet ordre proposé :

1. Justificatifs (photo ou PDF lié à l'opération).
2. Virements entre deux comptes du même utilisateur.
3. Budgets par catégorie et alerte en cas de dépassement.
4. Graphiques sur plusieurs périodes et export CSV / Excel.
5. Import du relevé bancaire (CSV ou OFX).
6. Application mobile.

**Ordre de développement de la V1 proposé :** comptes et périodes → catégories et libellés (avec la liste fournie) → opérations et saisie rapide → tableau de bord → récurrences.
