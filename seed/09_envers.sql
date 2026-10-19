-- 09: Envers baseline. The seed writes rows directly, so Hibernate Envers has no history for them and the first
-- update or delete through the app fails with "Cannot update previous revision". This file adds one revision
-- (revinfo, made by the admin user) and one REVTYPE 0 (ADD) row in each <table>_aud for every seeded row.
-- Columns are copied by name: every column that exists in both the table and its _aud table.
INSERT INTO revinfo (rev, revtstmp, user_id)
VALUES (nextval('revinfo_seq'), (extract(epoch FROM now()) * 1000)::bigint, pg_temp.uid(1, 1));

DO $$
DECLARE
    audited_table text;
    column_list   text;
BEGIN
    FOR audited_table IN
        SELECT replace(table_name, '_aud', '') FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name LIKE '%\_aud' ORDER BY 1
    LOOP
        SELECT string_agg(quote_ident(b.column_name), ', ') INTO column_list
        FROM information_schema.columns b
        JOIN information_schema.columns a
          ON a.table_schema = b.table_schema AND a.table_name = audited_table || '_aud' AND a.column_name = b.column_name
        WHERE b.table_schema = current_schema() AND b.table_name = audited_table;

        EXECUTE format(
            'INSERT INTO %I (%s, rev, revtype) SELECT %s, (SELECT max(rev) FROM revinfo), 0 FROM %I',
            audited_table || '_aud', column_list, column_list, audited_table);
    END LOOP;
END
$$;
