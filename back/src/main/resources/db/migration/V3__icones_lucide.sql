-- Charte « Sapin » : les icônes de catégorie sont des noms d'icônes Lucide et non plus des emoji,
-- et les catégories fournies prennent leurs couples de couleurs. Sur une base neuve la table n'existe
-- pas encore : rien à faire ici, l'initialisation des données fournies pose directement les nouvelles valeurs.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = current_schema() AND table_name = 'categorie') THEN
        RETURN;
    END IF;

    -- Catégories fournies : icône et couleur de la charte, repérées par leur nom
    UPDATE categorie c
    SET icone = v.icone, couleur = v.couleur
    FROM (VALUES
        ('logement', 'house', '#8A5A44'),
        ('courses', 'shopping-cart', '#9A6410'),
        ('sorties & restaurants', 'utensils', '#A86400'),
        ('abonnements & loisirs', 'tv', '#3F51A3'),
        ('téléphone & internet', 'smartphone', '#1F64B5'),
        ('assurances', 'shield-check', '#4A6378'),
        ('santé', 'heart-pulse', '#B83227'),
        ('soins & beauté', 'sparkles', '#B23A72'),
        ('carburant', 'fuel', '#B8501A'),
        ('véhicule', 'car', '#34449A'),
        ('prêts', 'percent', '#6B5240'),
        ('banque', 'landmark', '#4F5A66'),
        ('sport', 'dumbbell', '#0F7A70'),
        ('vacances & voyages', 'plane', '#0A779F'),
        ('maison & entretien', 'paint-roller', '#6B7A1E'),
        ('cadeaux', 'gift', '#A8335F'),
        ('autres dépenses', 'package', '#6B7079'),
        ('salaire', 'briefcase', '#1B5C42'),
        ('aides', 'handshake', '#1B5C42'),
        ('remboursements', 'undo-2', '#1B5C42'),
        ('autres revenus', 'piggy-bank', '#1B5C42')
    ) AS v(nom_normalise, icone, couleur)
    WHERE c.utilisateur_id IS NULL AND c.nom_normalise = v.nom_normalise;

    -- Catégories personnelles : l'emoji des catégories fournies devient l'icône correspondante,
    -- tout autre emoji devient l'icône neutre « package »
    UPDATE categorie c
    SET icone = COALESCE(v.icone, 'package')
    FROM categorie c2
    LEFT JOIN (VALUES
        ('🏠', 'house'), ('🛒', 'shopping-cart'), ('🍽️', 'utensils'), ('🍽', 'utensils'), ('🎬', 'tv'),
        ('📱', 'smartphone'), ('🛡️', 'shield-check'), ('🛡', 'shield-check'), ('🩺', 'heart-pulse'),
        ('💇', 'sparkles'), ('⛽', 'fuel'), ('🚗', 'car'), ('🏦', 'percent'), ('💳', 'landmark'),
        ('🏋️', 'dumbbell'), ('🏋', 'dumbbell'), ('✈️', 'plane'), ('✈', 'plane'), ('🧹', 'paint-roller'),
        ('🎁', 'gift'), ('📦', 'package'), ('💼', 'briefcase'), ('🤝', 'handshake'), ('↩️', 'undo-2'),
        ('↩', 'undo-2'), ('💰', 'piggy-bank')
    ) AS v(emoji, icone) ON v.emoji = c2.icone
    WHERE c.id = c2.id AND c.utilisateur_id IS NOT NULL
      AND c.icone IS NOT NULL AND c.icone !~ '^[a-z0-9-]+$';
END $$;
