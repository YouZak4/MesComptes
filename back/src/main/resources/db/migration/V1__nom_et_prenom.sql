-- Le pseudonyme (unique) devient un nom (obligatoire, non unique) et un prénom (facultatif).
-- Le pseudo existant est gardé comme nom. Sur une base neuve les tables n'existent pas encore :
-- rien à faire ici, Hibernate les crée ensuite avec les bonnes colonnes.
DO $$
DECLARE
    nom_table   text;
    contrainte  text;
BEGIN
    FOREACH nom_table IN ARRAY ARRAY['utilisateurs', 'inscriptions_en_attente'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = nom_table AND column_name = 'pseudo') THEN
            -- Contrainte d'unicité générée par Hibernate, au nom aléatoire
            FOR contrainte IN
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
                WHERE c.conrelid = nom_table::regclass AND c.contype = 'u' AND a.attname = 'pseudo'
            LOOP
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', nom_table, contrainte);
            END LOOP;

            IF EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = current_schema() AND table_name = nom_table AND column_name = 'nom') THEN
                -- Une ancienne colonne « nom » traîne déjà (version précédente du modèle) : le pseudo la remplace
                EXECUTE format('UPDATE %I SET nom = pseudo', nom_table);
                EXECUTE format('ALTER TABLE %I DROP COLUMN pseudo', nom_table);
                EXECUTE format('ALTER TABLE %I ALTER COLUMN nom SET NOT NULL', nom_table);
            ELSE
                EXECUTE format('ALTER TABLE %I RENAME COLUMN pseudo TO nom', nom_table);
            END IF;
            EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS prenom varchar(255)', nom_table);
        END IF;
    END LOOP;
END $$;
