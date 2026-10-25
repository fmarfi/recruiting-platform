package com.recruiting.platform.contracts;

import java.time.Instant;
import java.util.UUID;

/**
 * A CV or document was attached to a person, by a direct upload or from the CV intake queue
 * (B-01, since contract 1.2.0, ADR 0011). Publisher: accounts. Listeners: privacy (on a CV,
 * queue the KVKK notice and consent e-mail when {@code autoSendOnCvUpload} is on, S-01).
 *
 * @param eventId    event id
 * @param occurredAt time of the upload
 * @param actorId    the uploading user
 * @param personId   the person
 * @param fileId     the new {@code person_file} row
 * @param kind       CV or DOCUMENT
 * @param version    CV version number (1 for the first CV; always 1 for documents)
 */
public record PersonFileUploaded(UUID eventId, Instant occurredAt, UUID actorId,
                                 UUID personId, UUID fileId, PersonFileKind kind,
                                 int version) implements DomainEvent {
}
