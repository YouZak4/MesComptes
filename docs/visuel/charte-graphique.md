# Charte graphique MesComptes · « Sapin »

Version 1.1 · 21 octobre 2026 (ajouts : calendrier, libellés de la catégorie, « Autres » dépliable)
Référence visuelle : maquette « 7 · Sapin, menu en haut et bouton centré ».
Pendant HTML/CSS de ce document : [`charte-graphique.html`](./charte-graphique.html). Il contient aussi le fichier de variables CSS prêt à copier.

---

## 1. Intention

MesComptes doit inspirer **confiance et calme**, comme un outil financier sérieux. Il doit aussi rester **chaleureux** pour qu'on ait envie d'y revenir chaque semaine.

| On veut | On évite |
|---|---|
| Un vert profond et posé, des surfaces claires et aérées | L'aspect « banque froide » tout gris |
| Des angles arrondis généreux et des pastilles de couleur douces | L'aspect enfantin : emoji, Comic Sans, couleurs saturées partout |
| Des chiffres lisibles et alignés | Les dégradés décoratifs, les ombres lourdes, les bordures colorées sur un côté |
| Une seule action forte par écran : le bouton jaune | Plusieurs boutons qui se disputent l'attention |

Trois principes :
1. **Le chiffre d'abord.** Les montants sont l'information principale : gros, gras et alignés.
2. **Une couleur, une signification.** Le vert porte la marque, le jaune porte l'action, le rouge brique signale une sortie d'argent. Les teintes de catégories ne servent qu'à reconnaître une catégorie.
3. **Le calme par l'espace.** On sépare les blocs par des cartes blanches sur fond vert très pâle, pas par des traits.

---

## 2. Couleurs

### 2.1 Marque

| Jeton | Hex | Usage |
|---|---|---|
| `--vert-700` · **Sapin** | `#1B5C42` | Couleur principale : barre de menu, liens, revenus, icônes de marque |
| `--vert-800` · Sapin foncé | `#154A35` | Survol des éléments verts, zones sombres dans la barre |
| `--vert-900` · Forêt | `#123F2E` | Carte « Solde de la période », titres de page |
| `--vert-300` · Sauge | `#C9E0D3` | Texte secondaire **sur fond vert** (liens inactifs du menu) |
| `--vert-200` · Menthe | `#D8EBDF` | Logo, avatar, pastilles sur fond vert |
| `--vert-100` · Brume | `#DCEFE4` | Pastille de l'indicateur « Revenus » |
| `--vert-50` · Fond | `#F3F6F4` | Fond de page, onglet actif du menu, encarts internes |

### 2.2 Action

| Jeton | Hex | Usage |
|---|---|---|
| `--jaune-500` · **Doré** | `#E9B949` | Fond du bouton principal « Nouvelle opération » et de lui seul |
| `--jaune-600` | `#D9A731` | Survol du bouton doré |
| `--encre-jaune` | `#17140C` | Texte et icône sur le doré |

> Le doré est **réservé à l'action principale**. Un écran ne compte qu'un seul bouton doré.

### 2.3 Neutres

| Jeton | Hex | Usage |
|---|---|---|
| `--encre` | `#17241E` | Texte principal (vert presque noir, jamais du noir pur) |
| `--encre-douce` | `#55665D` | Texte secondaire, libellés, dates, méta |
| `--trait` | `#E1E8E4` | Séparateurs, bordures de champ au repos |
| `--blanc` | `#FFFFFF` | Cartes, champs, fenêtres |

### 2.4 Sémantique (montants)

| Jeton | Hex | Usage |
|---|---|---|
| `--positif` | `#1B5C42` | Montant entrant (revenus), même vert que la marque |
| `--negatif` | `#A8352A` | Montant sortant **mis en avant** (soldes négatifs, prévisions) |
| `--negatif-fond` | `#F8E3DF` | Pastille de l'indicateur « Dépenses », message d'erreur |
| `--negatif-sur-sombre` | `#FFB8AD` | Solde négatif sur la carte Forêt |
| `--alerte` | `#9A6410` | Avertissement (ex. : budget presque atteint) |
| `--alerte-fond` | `#F8EBD3` | Fond d'avertissement |

Règle : dans les listes d'opérations, le montant reste en `--encre`, précédé du signe moins (−). On ne le colore pas en rouge, car une liste toute rouge ne veut plus rien dire. Le rouge brique sert aux **soldes et totaux**.

### 2.5 Catégories

Chaque catégorie a une **couleur** (trait de l'icône, segment du graphique) et une **teinte** (fond de pastille). Les couleurs sont assez sombres pour rester lisibles sur leur teinte (contraste ≥ 4,5:1). Aucune n'est proche du vert de marque.

| Catégorie | Icône (Lucide) | Couleur | Teinte |
|---|---|---|---|
| Abonnements & loisirs | `tv` | `#3F51A3` | `#E4E8F6` |
| Assurances | `shield-check` | `#4A6378` | `#E3E9EE` |
| Autres dépenses | `package` | `#6B7079` | `#ECEDEF` |
| Banque | `landmark` | `#4F5A66` | `#E7EAED` |
| Cadeaux | `gift` | `#A8335F` | `#F6E3EA` |
| Carburant | `fuel` | `#B8501A` | `#F8E4D8` |
| Courses | `shopping-cart` | `#9A6410` | `#F8EBD3` |
| Logement | `house` | `#8A5A44` | `#F1E6E0` |
| Maison & entretien | `paint-roller` | `#6B7A1E` | `#EEF0D9` |
| Prêts | `percent` | `#6B5240` | `#EEE7E1` |
| Santé | `heart-pulse` | `#B83227` | `#F8E1DF` |
| Soins & beauté | `sparkles` | `#B23A72` | `#F7E2EC` |
| Sorties & restaurants | `utensils` | `#A86400` | `#FBEBD0` |
| Sport | `dumbbell` | `#0F7A70` | `#DAF0ED` |
| Téléphone & internet | `smartphone` | `#1F64B5` | `#E0EBF8` |
| Vacances & voyages | `plane` | `#0A779F` | `#DCEFF6` |
| Véhicule | `car` | `#34449A` | `#E3E6F5` |

Les catégories de **revenus** utilisent toutes le couple marque : couleur `#1B5C42`, teinte `#DCEFE4`.
Pour une catégorie créée par l'utilisateur, on propose ces 17 couples dans le sélecteur de couleur, sans saisie libre.

---

## 3. Typographie

**Police unique : [Manrope](https://fonts.google.com/specimen/Manrope)** (Google Fonts, graisses 400 à 800).
Repli : `system-ui, -apple-system, "Segoe UI", sans-serif`.

| Style | Taille / interligne | Graisse | Espacement | Usage |
|---|---|---|---|---|
| Titre de page | 30 px / 1.2 | 800 | −0,025 em | « Octobre 2026 » (couleur Forêt) |
| Montant héros | 32 px / 1.1 | 800 | −0,02 em | Solde de la période |
| Montant indicateur | 24 px / 1.2 | 800 | 0 | Revenus, Dépenses |
| Titre de carte | 16 px / 1.3 | 800 | 0 | « Dépenses par catégorie » |
| Marque | 17 px | 800 | −0,01 em | « Mes comptes » dans la barre |
| Corps fort | 14 px / 1.45 | 700 | 0 | Libellé d'opération, nom de catégorie, menu actif |
| Corps | 14 px / 1.45 | 500 | 0 | Texte courant |
| Méta | 12 px / 1.4 | 500–600 | 0 | Date, catégorie, note sous un libellé |

Règles :
- **Tous les montants** utilisent `font-variant-numeric: tabular-nums` pour que les chiffres s'alignent en colonne.
- Format français : `1 234,50 €` avec une espace fine insécable (U+202F) pour les milliers, une virgule pour les décimales et une espace insécable avant « € ».
- Le signe négatif est le vrai moins `−` (U+2212), pas un tiret.
- Pas d'italique et pas de majuscules continues, sauf de rares surtitres en 12 px.

---

## 4. Espacements, rayons, ombres

### Espacements (base 4 px)
`4 · 8 · 12 · 14 · 16 · 22 · 24 · 32 · 40` px.

| Contexte | Valeur |
|---|---|
| Marge intérieure d'une carte | 22 px |
| Écart entre cartes | 16 px |
| Écart entre sections de page | 22 px |
| Écart icône ↔ texte dans une ligne | 12 px |
| Hauteur d'une ligne d'opération | 11 px de marge verticale + pastille 38 px |
| Marges latérales de page | 24 px (16 px sur mobile) |
| Largeur max du contenu | 1180 px, centré |

### Rayons
| Jeton | Valeur | Usage |
|---|---|---|
| `--rayon-s` | 10 px | Boutons icône, petits contrôles |
| `--rayon-m` | 12 px | Boutons, champs, pastilles de catégorie, onglets du menu |
| `--rayon-l` | 14 px | Encarts internes, sélecteur de période, pastilles d'indicateur |
| `--rayon-xl` | 18 px | Cartes |
| `--rayon-rond` | 50 % | Avatar circulaire, anneau du graphique |

### Ombres
| Jeton | Valeur | Usage |
|---|---|---|
| `--ombre-carte` | `0 1px 2px #123F2E14` | Cartes et sélecteur de période, à peine visible |
| `--ombre-flottante` | `0 6px 18px #123F2E33` | Bouton « Nouvelle opération » flottant |
| `--ombre-fenetre` | `0 24px 48px #123F2E33` | Fenêtre modale |

On n'empile jamais une bordure et une ombre sur une carte.

---

## 5. Iconographie

- **Jeu : [Lucide](https://lucide.dev)**, à installer avec `lucide-vue-next` côté Vue. Il remplace **tous les emoji**, y compris celui du mot de passe (`eye` / `eye-off`).
- Grille de 24 px, trait **1,9 px** (2,2 à 2,6 px pour les petites icônes de bouton), extrémités et jonctions **arrondies**, pas de remplissage.
- Tailles : 17–18 px dans le menu et les boutons, 19 px dans une pastille de catégorie, 20 px dans une pastille d'indicateur.
- **Pastille de catégorie** : carré de 38 px, rayon 12 px, fond = teinte, icône = couleur de la catégorie.
- **Pastille d'indicateur** : carré de 44 px, rayon 14 px. Revenus : `arrow-up` en vert sur Brume. Dépenses : `arrow-down` en brique sur `--negatif-fond`.
- Une icône seule dans un bouton porte toujours un `aria-label`. Une icône décorative porte `aria-hidden="true"`.

Icônes d'interface :

| Fonction | Icône Lucide |
|---|---|
| Tableau de bord | `layout-dashboard` |
| Opérations | `list` |
| Récurrences | `repeat` |
| Réglages | `sliders-vertical` |
| Déconnexion | `log-out` |
| Période précédente / suivante | `chevron-left` / `chevron-right` |
| Nouvelle opération | `plus` |
| Logo | `trending-up` sur pastille Menthe |

---

## 6. Mise en page

```
┌──────────────────────────────────────────────────────────────┐
│ ▣ Mes comptes  [Tableau de bord] Opérations Récurrences …  Y │  barre Sapin, pleine largeur
├──────────────────────────────────────────────────────────────┤
│  Octobre 2026                               ‹ Période en cours › │
│  du 25 septembre au 25 octobre 2026                          │
│                                                              │
│  [ Solde (Forêt) ] [ Revenus ] [ Dépenses ]                  │  3 indicateurs
│                                                              │
│  [ Dépenses par catégorie ] [ Dernières opérations   ]       │  2 colonnes ≥ 400 px
│  [ anneau + légende       ] [ À venir sur la période ]       │
│                                                              │
│                  ( + Nouvelle opération )                    │  doré, centré, collant en bas
└──────────────────────────────────────────────────────────────┘
```

- **Barre de menu** : fond Sapin, pleine largeur, contenu centré sur 1180 px, hauteur ≈ 64 px. Logo à gauche, onglets ensuite, utilisateur et déconnexion à droite.
- **Onglet actif** : pilule `--vert-50` avec texte Sapin en graisse 700. Onglets inactifs : texte Sauge en graisse 600, sans fond.
- **En-tête de période** : titre et dates à gauche. À droite, le sélecteur de période dans une capsule blanche (flèches + « Période en cours »).
- **Indicateurs** : grille `repeat(auto-fit, minmax(210px, 1fr))`. Le solde vient en premier, sur fond Forêt.
- **Contenu** : deux colonnes souples (`flex: 1 1 400px`) qui passent l'une sous l'autre sous ≈ 860 px.
- **Bouton « Nouvelle opération »** : centré, `position: sticky; bottom: 24px`, toujours visible.
- **Mobile (< 640 px)** : les onglets passent à la ligne (ou icônes seules). Les indicateurs s'empilent. Le bouton doré prend toute la largeur moins 16 px de chaque côté.

---

## 7. Composants

### 7.1 Boutons

| Variante | Fond | Texte | Rayon | Hauteur | Usage |
|---|---|---|---|---|---|
| **Principal (doré)** | `#E9B949` | `#17140C`, 800 | 12 px | 46 px | « Nouvelle opération », « Enregistrer ». Un seul par écran |
| **Secondaire** | `#FFFFFF` + bordure 1 px `--trait` | `--encre`, 700 | 12 px | 44 px | « Enregistrer et en ajouter une autre » |
| **Discret** | transparent | Sapin, 700 | 12 px | 44 px | « Annuler », « Tout voir » |
| **Icône** | transparent ou blanc | selon le fond | 10 px | 40 × 40 px | Flèches de période, déconnexion |
| **Danger** | `#A8352A` | blanc, 700 | 12 px | 44 px | Suppression confirmée |

États : au survol, le fond fonce d'un cran (doré → `#D9A731`, blanc → `--vert-50`). Au focus, contour `2px solid #1B5C42` décalé de 2 px. Désactivé : opacité 0,45 et curseur `not-allowed`. Cible tactile minimale de 44 px.

### 7.2 Cartes
Fond blanc, rayon 18 px, marge intérieure 22 px, `--ombre-carte`, sans bordure. Titre en 16 px / 800, suivi d'un lien discret « Tout voir » aligné à droite si besoin.
**Carte héros** (solde) : fond Forêt `#123F2E`, libellé en `#B9D6C6`, montant 32 px en blanc (ou `--negatif-sur-sombre` s'il est négatif).

### 7.3 Indicateur
Carte blanche : pastille d'indicateur de 44 px, libellé en `--encre-douce` (600), montant 24 px / 800 dessous.

### 7.4 Ligne d'opération
`[pastille catégorie 38] [libellé 700 / méta 12 px] ……… [−47,50 € 800]`
Méta : `13 oct. · Courses · note`. Pas de séparateur entre les lignes, juste 11 px de marge verticale. Au survol, fond `--vert-50`.

### 7.5 Répartition par catégorie
Anneau de 140 px (épaisseur 20 px) fait avec `conic-gradient`, segments aux couleurs des catégories, total au centre (« Total » en 12 px et montant en 17 px / 800). À droite, une légende : pastille, nom, montant, puis « 48 % des dépenses » en méta. Au-delà de 6 catégories, les plus petites sont regroupées en « Autres ».
Après la part, la méta donne l'écart avec la période précédente : `+35,00 € vs période précédente`, en brique si défavorable et en Sapin si favorable, ou « Identique à la période précédente » en `--encre-douce`.

**« Autres » dépliable** : toute la ligne est un bouton (`aria-expanded`). La méta se termine par « · **Voir les 9 catégories** » en Sapin 700 (« Masquer… » une fois déplié), souligné au survol, et un chevron `chevron-down` de 18 px en Sapin, à droite, pivote de 180° à l'ouverture. Au survol, fond `--vert-50`, rayon 12 px. Le détail s'affiche dessous en retrait de 18 px, rattaché par un filet de 2 px `--trait` à gauche, avec la même présentation que les autres lignes. Une fois la légende allongée, l'anneau garde exactement la place qu'il avait avant le dépliage : il ne remonte pas sous le titre.

### 7.6 Encart interne
Fond `--vert-50`, rayon 14 px, marge intérieure 12 × 14 px. Il sert aux prévisions (« Solde prévu », « Comptes en fin de période »), sur une grille de deux colonnes.

### 7.7 Champs de formulaire
- Libellé au-dessus, 13 px / 700, `--encre`.
- Champ : fond blanc, bordure 1 px `#C9D6CE`, rayon 12 px, hauteur 46 px, marge intérieure 0 × 14 px, texte 15 px.
- Focus : bordure Sapin et halo `0 0 0 3px #1B5C4226`.
- Erreur : bordure `#A8352A`, message en 12 px brique sous le champ, précédé de l'icône `circle-alert`.
- **Champ montant** : 28 px / 800, chiffres tabulaires, « € » en suffixe dans le champ en `--encre-douce`.
- **Champ date** : un bouton qui ouvre le calendrier de l'application, voir 7.11.
- On n'utilise plus de fond violet ni de fond coloré pour les champs.

### 7.8 Fenêtre modale (ex. Nouvelle opération)
Voile `#123F2E66`. Fenêtre blanche, rayon 18 px, marge intérieure 28 px, largeur 520 px, `--ombre-fenetre`. Titre 20 px / 800. Pied de fenêtre aligné à droite : Discret « Annuler », Secondaire « Enregistrer et en ajouter une autre », Principal doré « Enregistrer ».

### 7.9 Pastilles d'état (badges)
Hauteur 24 px, rayon 999 px, 12 px / 700. Variantes : neutre (`--vert-50` / Sapin), alerte (`--alerte-fond` / `--alerte`), danger (`--negatif-fond` / `--negatif`). Exemples : « Récurrente », « Masquée ».

### 7.10 États vides
Texte `--encre-douce` centré dans la carte, avec une icône Lucide de 28 px en `#9DB0A5` au-dessus et un lien d'action Sapin si une action existe. Exemple : « Aucune opération récurrente à venir. Ajouter une récurrence ».

### 7.11 Champ date et calendrier
Le sélecteur de date du navigateur n'est jamais utilisé : tous les champs date ouvrent le calendrier de l'application.

- **Champ** : même allure qu'un champ de saisie (7.7), mais c'est un bouton. Icône `calendar-days` de 18 px en Sapin, puis la date en clair : `sam. 25 juillet 2026`. Sans date (champ facultatif) : « Aucune date » en `#7B8B82`.
- **Calendrier** : carte blanche de 320 px, rayon 14 px, marge intérieure 14 px, `--ombre-fenetre`. Il s'ouvre sous le champ, ou au-dessus quand la place manque, et s'aligne à droite quand il dépasserait de l'écran.
- **En-tête** : boutons icône `chevron-left` / `chevron-right` (7.1), au centre le nom de la période ou du mois en 15 px / 800, puis ses bornes en méta : `du 25 juil. au 24 août`.
- **Grille** : semaines entières du lundi au dimanche, en-têtes `L M M J V S D` en 11 px / 700 `--encre-douce`. Jours de 40 px de haut, rayon 10 px, 14 px / 600, chiffres tabulaires.
- **Fenêtre affichée** : dans la saisie d'une opération, une période entière, même à cheval sur deux mois. Ailleurs, ou hors des périodes connues, un mois civil. Un petit repère du mois en 9 px / 700 majuscules Sapin (`JUIL`, `AOÛT`) surmonte le premier jour de la fenêtre et chaque 1er du mois.
- **États des jours** :

| État | Rendu |
|---|---|
| Survol | fond `--vert-50`, texte Sapin |
| Aujourd'hui | anneau intérieur 1,5 px `--vert-300`, graisse 800 |
| Choisi | fond Sapin, texte blanc, repère du mois en `--vert-200` |
| Hors de la période (complète les semaines) | `#A3B1A9`, graisse 500, reste cliquable |
| Avant la date minimale | opacité 0,35, non cliquable |
| Focus clavier | anneau 2 px Sapin |

- **Pied** : filet `--trait`, puis boutons discrets « Aujourd'hui » et, pour un champ facultatif, « Effacer ».

### 7.12 Libellés de la catégorie
Sous le champ Catégorie, tant qu'aucun libellé existant n'est choisi : « Libellés de cette catégorie » en méta, puis un bouton pilule par libellé (rayon 999 px, bordure 1 px `--trait-champ`, fond blanc, 13 px / 600, marge intérieure 6 × 12 px). Au survol et au focus, bordure Sapin, fond `--vert-50` et texte Sapin. Les pilules passent à la ligne. Au-delà d'environ quatre lignes, la zone défile.

---

## 8. Écran de connexion
Fond `--vert-50`. Carte blanche centrée de 420 px (rayon 18 px, marge intérieure 32 px). Dessus : logo et « Mes comptes » en Sapin, puis l'accroche « Votre outil de suivi des dépenses » en `--encre-douce`. Champs selon 7.7. L'œil du mot de passe utilise `eye` / `eye-off` de Lucide. « Connexion » est un bouton doré pleine largeur, « Créer un nouveau compte » un bouton discret dessous.

---

## 9. Ton et rédaction
- On tutoie ou on reste neutre de façon cohérente. L'interface actuelle est neutre (« Se connecter »), on garde ce ton.
- Libellés courts, verbe à l'infinitif pour les actions : « Enregistrer », « Masquer », « Tout voir ».
- Dates abrégées en liste (`13 oct.`), complètes en en-tête (`du 25 septembre au 25 octobre 2026`).
- Écart avec la période précédente : `+35,00 € vs période précédente`. S'il est nul : « Identique à la période précédente », jamais un signe « = ».
- Jamais d'emoji dans l'interface.

---

## 10. Accessibilité
- Contraste minimal de 4,5:1 pour le texte, 3:1 pour le texte ≥ 24 px et pour les icônes. Toutes les paires de cette charte le respectent.
- Les couleurs de catégorie ne sont jamais le seul indice : le nom de la catégorie est toujours écrit à côté.
- Focus clavier toujours visible (contour Sapin de 2 px).
- Vrais éléments `<button>`, `<a>`, `<label>` et `<input>`. L'anneau du graphique porte un `role="img"` et un `aria-label` qui récapitule la répartition.
- Calendrier au clavier : flèches pour changer de jour ou de semaine, Pg préc. / Pg suiv. pour changer de période, Entrée pour choisir, Échap pour fermer le calendrier sans fermer la fenêtre. Le focus revient ensuite sur le champ.

---

## 11. Variables CSS

À placer dans `front/src/styles/theme.css` et à importer dans `main.ts`.

```css
:root {
  /* Marque */
  --vert-900: #123F2E;
  --vert-800: #154A35;
  --vert-700: #1B5C42;
  --vert-300: #C9E0D3;
  --vert-200: #D8EBDF;
  --vert-100: #DCEFE4;
  --vert-50:  #F3F6F4;

  /* Action */
  --jaune-500: #E9B949;
  --jaune-600: #D9A731;
  --encre-jaune: #17140C;

  /* Neutres */
  --encre: #17241E;
  --encre-douce: #55665D;
  --trait: #E1E8E4;
  --trait-champ: #C9D6CE;
  --blanc: #FFFFFF;

  /* Sémantique */
  --positif: #1B5C42;
  --negatif: #A8352A;
  --negatif-fond: #F8E3DF;
  --negatif-sur-sombre: #FFB8AD;
  --alerte: #9A6410;
  --alerte-fond: #F8EBD3;

  /* Typo */
  --police: 'Manrope', system-ui, -apple-system, 'Segoe UI', sans-serif;

  /* Rayons */
  --rayon-s: 10px;
  --rayon-m: 12px;
  --rayon-l: 14px;
  --rayon-xl: 18px;

  /* Ombres */
  --ombre-carte: 0 1px 2px #123F2E14;
  --ombre-flottante: 0 6px 18px #123F2E33;
  --ombre-fenetre: 0 24px 48px #123F2E33;

  /* Mise en page */
  --largeur-contenu: 1180px;
}
```

Les couleurs de catégories gagnent à vivre côté données (référentiel des catégories) plutôt qu'en CSS. Le couple `couleur` / `teinte` du tableau 2.5 sert de valeur par défaut.
