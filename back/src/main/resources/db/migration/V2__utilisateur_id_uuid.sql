-- L'id des utilisateurs passe d'un compteur (bigint) à un UUID, et toutes les clés étrangères utilisateur_id suivent.
-- Chaque utilisateur reçoit un UUID aléatoire ; les lignes qui lui appartiennent sont reliées au même UUID.
-- Les contraintes et index qui portent sur ces colonnes sont relevés puis recréés à l'identique.
-- Sur une base neuve (ou déjà migrée), rien à faire : Hibernate crée les colonnes directement en uuid.
DO $$
DECLARE
    fk          record;
    definition  record;
    cle_primaire text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = 'utilisateurs'
                     AND column_name = 'id' AND data_type = 'bigint') THEN
        RETURN;
    END IF;

    ALTER TABLE utilisateurs ADD COLUMN nouvel_id uuid NOT NULL DEFAULT gen_random_uuid();

    -- Clés étrangères vers utilisateurs : table, colonne, nom et caractère obligatoire
    CREATE TEMP TABLE migration_fk ON COMMIT DROP AS
    SELECT c.conname::text AS nom, c.conrelid::regclass::text AS nom_table,
           a.attname::text AS colonne, a.attnotnull AS obligatoire
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
    WHERE c.contype = 'f' AND c.confrelid = 'utilisateurs'::regclass;

    -- Autres contraintes (clé primaire, unicité…) qui utilisent ces colonnes : supprimées avec la colonne, à recréer
    CREATE TEMP TABLE migration_contraintes ON COMMIT DROP AS
    SELECT c.conrelid::regclass::text AS nom_table, c.conname::text AS nom, pg_get_constraintdef(c.oid) AS definition
    FROM pg_constraint c
    JOIN migration_fk f ON f.nom_table = c.conrelid::regclass::text
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = f.colonne
    WHERE c.contype <> 'f' AND a.attnum = ANY (c.conkey);

    -- Index sans contrainte sur ces colonnes, idem
    CREATE TEMP TABLE migration_index ON COMMIT DROP AS
    SELECT pg_get_indexdef(i.indexrelid) AS definition
    FROM pg_index i
    JOIN migration_fk f ON f.nom_table = i.indrelid::regclass::text
    JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attname = f.colonne
    WHERE a.attnum = ANY (i.indkey::smallint[])
      AND NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conindid = i.indexrelid);

    -- Chaque colonne utilisateur_id reçoit l'UUID de son utilisateur, puis remplace l'ancienne
    FOR fk IN SELECT * FROM migration_fk LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN migration_uuid uuid', fk.nom_table);
        EXECUTE format('UPDATE %I t SET migration_uuid = u.nouvel_id FROM utilisateurs u WHERE u.id = t.%I',
                       fk.nom_table, fk.colonne);
        EXECUTE format('ALTER TABLE %I DROP COLUMN %I', fk.nom_table, fk.colonne);
        EXECUTE format('ALTER TABLE %I RENAME COLUMN migration_uuid TO %I', fk.nom_table, fk.colonne);
        IF fk.obligatoire THEN
            EXECUTE format('ALTER TABLE %I ALTER COLUMN %I SET NOT NULL', fk.nom_table, fk.colonne);
        END IF;
    END LOOP;

    -- La table utilisateurs elle-même : l'UUID devient la clé primaire, l'UUID est désormais généré par l'application
    SELECT conname INTO cle_primaire FROM pg_constraint
    WHERE conrelid = 'utilisateurs'::regclass AND contype = 'p';
    ALTER TABLE utilisateurs DROP COLUMN id;
    ALTER TABLE utilisateurs RENAME COLUMN nouvel_id TO id;
    ALTER TABLE utilisateurs ALTER COLUMN id DROP DEFAULT;
    EXECUTE format('ALTER TABLE utilisateurs ADD CONSTRAINT %I PRIMARY KEY (id)', cle_primaire);

    -- Recréation des contraintes et index relevés, puis des clés étrangères
    FOR definition IN SELECT * FROM migration_contraintes LOOP
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I %s', definition.nom_table, definition.nom, definition.definition);
    END LOOP;
    FOR definition IN SELECT * FROM migration_index LOOP
        EXECUTE definition.definition;
    END LOOP;
    FOR fk IN SELECT * FROM migration_fk LOOP
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES utilisateurs (id)',
                       fk.nom_table, fk.nom, fk.colonne);
    END LOOP;
END $$;
