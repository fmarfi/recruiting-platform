-- Contract 1.1.1: index for the 'city' filter on GET /people (accent- and case-insensitive equality).
CREATE INDEX idx_person_city ON person (immutable_unaccent(lower(trim(city))));
