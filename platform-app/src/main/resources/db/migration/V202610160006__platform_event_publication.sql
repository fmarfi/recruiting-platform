-- Spring Modulith 2.1.x JPA event publication registry (spring-modulith-starter-jpa).
-- ddl-auto is "validate", so nothing creates these automatically. Matches the schema shipped with Modulith.
CREATE TABLE event_publication (
    id                     uuid                     NOT NULL,
    listener_id            text                     NOT NULL,
    event_type             text                     NOT NULL,
    serialized_event       text                     NOT NULL,
    publication_date       timestamp with time zone NOT NULL,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamp with time zone,
    PRIMARY KEY (id)
);
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);

-- Used when spring.modulith.events.completion-mode=archive
CREATE TABLE event_publication_archive (
    id                     uuid                     NOT NULL,
    listener_id            text                     NOT NULL,
    event_type             text                     NOT NULL,
    serialized_event       text                     NOT NULL,
    publication_date       timestamp with time zone NOT NULL,
    completion_date        timestamp with time zone,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamp with time zone,
    PRIMARY KEY (id)
);
CREATE INDEX event_publication_archive_serialized_event_hash_idx
    ON event_publication_archive USING hash (serialized_event);
CREATE INDEX event_publication_archive_by_completion_date_idx ON event_publication_archive (completion_date);
