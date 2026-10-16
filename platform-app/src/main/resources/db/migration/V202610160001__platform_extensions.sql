-- Extensions and search helper (A-14). pgvector ("vector") is deferred to Phase 2.
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() is only STABLE, so it cannot be used in an index expression. This wrapper pins the
-- dictionary and is declared IMMUTABLE. Dotless i and dotted I are folded first because the
-- default unaccent rules do not cover them (Turkish names).
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, translate($1, U&'\0131\0130', 'iI')) $$;
