package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.contracts.FileStore;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 0 of the erasure order (schema.md): collect the merged ids and the file keys of a person before anything is
 * deleted, because on DELETE the cascade removes {@code person_merge} and {@code person_file} rows. Also deletes file
 * bytes from the {@link FileStore} after the commit that removed the rows, and discards pending CV intake items
 * that belong to a data subject (matched by their lower-cased detected e-mails, ADR 0011 decision 5).
 * Reads other modules' tables for ids and keys only: accounts has no API for it yet.
 */
@Component
public class ErasureScopes {

    private static final Logger LOG = LoggerFactory.getLogger(ErasureScopes.class);
    private static final String EMPTY_DETECTED = "{\"emails\":[],\"phones\":[],\"linkedinUrls\":[]}";

    /** The ids and keys of one erasure. */
    public record Scope(UUID personId, List<UUID> mergedIds, List<String> fileKeys) {
    }

    private final JdbcTemplate jdbc;
    private final ObjectProvider<FileStore> files;

    ErasureScopes(JdbcTemplate jdbc, ObjectProvider<FileStore> files) {
        this.jdbc = jdbc;
        this.files = files;
    }

    public Scope collect(UUID personId) {
        List<UUID> merged = jdbc.queryForList("select merged_id from person_merge where survivor_id = ?", UUID.class,
                personId);
        List<String> keys = jdbc.queryForList("select storage_key from person_file where person_id = ? "
                + "union select storage_key from cv_intake_item where attached_person_id = ? "
                + "and storage_key is not null", String.class, personId, personId);
        return new Scope(personId, merged, keys);
    }

    /** Deletes the bytes; call only after the transaction that removed the rows has committed. */
    public void deleteBytes(List<String> keys) {
        FileStore store = files.getIfAvailable();
        if (store == null) {
            return;
        }
        for (String key : keys) {
            try {
                store.delete(key);
            } catch (IOException | RuntimeException ex) {
                LOG.warn("Could not delete stored file {}: {}", key, ex.getClass().getSimpleName());
            }
        }
    }

    /** Minimises intake items attached to the person (their bytes are in {@link Scope#fileKeys()}). */
    public void minimiseAttachedIntake(UUID personId) {
        jdbc.update("update cv_intake_item set file_name = null, extracted_text = null, storage_key = null, "
                + "sha256 = null, detected = cast(? as jsonb) where attached_person_id = ?", EMPTY_DETECTED, personId);
    }

    /** Discards pending intake items whose detected e-mails contain this address (any case). Returns the count. */
    public int discardPendingByEmail(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        String lower = email.trim().toLowerCase(java.util.Locale.ROOT);
        String match = "status = 'PENDING' and detected @> jsonb_build_object('emails', "
                + "jsonb_build_array(cast(? as text)))";
        List<String> keys = jdbc.queryForList("select storage_key from cv_intake_item where " + match
                + " and storage_key is not null", String.class, lower);
        int n = jdbc.update("update cv_intake_item set status = 'DISCARDED', file_name = null, extracted_text = null, "
                + "storage_key = null, sha256 = null, detected = cast(? as jsonb) where " + match, EMPTY_DETECTED,
                lower);
        deleteBytes(keys);
        return n;
    }
}
