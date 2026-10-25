-- pipeline (Wave 2a, ADR 0010): bulk_action and bulk_action_item. Append-only, not audited.
CREATE TABLE bulk_action (
    id              uuid PRIMARY KEY,
    entity          varchar(40) NOT NULL
        CHECK (entity IN ('PEOPLE', 'ACCOUNTS', 'JOB_ORDERS', 'PROPOSALS')),
    action          varchar(40) NOT NULL
        CHECK (action IN ('REASSIGN_OWNER', 'CHANGE_STATUS', 'ADD_TAG', 'REMOVE_TAG', 'SET_NEXT_ACTION', 'REQUEST_CONSENT')),
    params          jsonb       NOT NULL,
    actor_id        uuid        NOT NULL REFERENCES app_user (id),
    started_at      timestamptz NOT NULL,
    finished_at     timestamptz NOT NULL,
    requested_count int         NOT NULL,
    ok_count        int         NOT NULL,
    skipped_count   int         NOT NULL,
    failed_count    int         NOT NULL
);
CREATE INDEX idx_bulk_action_actor ON bulk_action (actor_id, started_at DESC);

CREATE TABLE bulk_action_item (
    bulk_action_id uuid        NOT NULL REFERENCES bulk_action (id) ON DELETE CASCADE,
    entity_id      uuid        NOT NULL,
    outcome        varchar(40) NOT NULL CHECK (outcome IN ('OK', 'SKIPPED', 'FAILED')),
    code           varchar(40)
        CHECK (code IN ('NO_CHANGE', 'NOT_FOUND', 'FORBIDDEN', 'ILLEGAL_TRANSITION', 'TRANSITION_GUARD_FAILED',
                        'REASON_REQUIRED', 'STATE_CONFLICT', 'VERSION_CONFLICT', 'NO_EMAIL', 'CONSENT_GIVEN',
                        'CONSENT_REFUSED', 'CONSENT_WITHDRAWN', 'REQUEST_PENDING', 'ANONYMISED', 'NO_CONSENT_BASIS',
                        'TOO_MANY_TAGS', 'INTERNAL_ERROR')),
    message        varchar(500),
    PRIMARY KEY (bulk_action_id, entity_id),
    CONSTRAINT ck_bulk_item_code CHECK ((outcome = 'OK' AND code IS NULL) OR (outcome <> 'OK' AND code IS NOT NULL))
);
CREATE INDEX idx_bulk_action_item_entity ON bulk_action_item (entity_id);
