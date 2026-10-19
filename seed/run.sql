-- Loads the whole demo dataset in one transaction. Run from inside the seed directory (load.sh / load.ps1 do this).
-- Temp tables and functions created by earlier files are reused by later ones, so keep the numeric order.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
\i 00_clear.sql
\i 01_users.sql
\i 02_accounts.sql
\i 03_people.sql
\i 04_job_orders.sql
\i 05_proposals.sql
\i 06_activities_actions.sql
\i 07_timeline.sql
\i 08_privacy.sql
\i 09_envers.sql
COMMIT;
