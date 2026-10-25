-- accounts (Wave 2a, ADR 0010): saved_view. Not audited (a user setting).
CREATE TABLE saved_view (
    id          uuid PRIMARY KEY,
    entity      varchar(40)  NOT NULL
        CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS')),
    name        varchar(120) NOT NULL,
    visibility  varchar(40)  NOT NULL DEFAULT 'PRIVATE'
        CHECK (visibility IN ('PRIVATE', 'SHARED')),
    owner_id    uuid         NOT NULL REFERENCES app_user (id),
    definition  jsonb        NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    created_by  uuid REFERENCES app_user (id),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  uuid REFERENCES app_user (id),
    version     bigint       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_saved_view_owner_entity_name ON saved_view (owner_id, entity, lower(name));
CREATE INDEX idx_saved_view_owner_entity ON saved_view (owner_id, entity);
CREATE INDEX idx_saved_view_entity_visibility ON saved_view (entity, visibility);
