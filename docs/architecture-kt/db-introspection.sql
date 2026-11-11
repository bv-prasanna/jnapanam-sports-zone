-- Jnapanam READ-ONLY database structure verification.
-- Run with a read-only PostgreSQL account against a confirmed target.
-- Never use this script to copy customer data or secrets.
BEGIN TRANSACTION READ ONLY;
SELECT current_database() AS database_name, current_schema() AS schema_name,
       current_user AS connecting_role, now() AS checked_at;
SELECT installed_rank, version, description, type, script, checksum,
       installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank;
SELECT table_schema, table_name, column_name, data_type, is_nullable,
       column_default
FROM information_schema.columns
WHERE table_schema NOT IN ('information_schema','pg_catalog')
ORDER BY table_schema,table_name,ordinal_position;
SELECT ns.nspname AS schema_name, cls.relname AS table_name,
       con.conname AS constraint_name, con.contype AS type,
       pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con
JOIN pg_class cls ON cls.oid=con.conrelid
JOIN pg_namespace ns ON ns.oid=cls.relnamespace
WHERE ns.nspname NOT IN ('information_schema','pg_catalog')
ORDER BY ns.nspname,cls.relname,con.conname;
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname NOT IN ('information_schema','pg_catalog')
ORDER BY schemaname,tablename,indexname;
COMMIT;
