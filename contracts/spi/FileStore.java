package com.recruiting.platform.contracts;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Binary file storage behind CVs, documents and the CV intake queue (since contract 1.2.0,
 * ADR 0011). Modules store only the opaque {@code key} in their tables; bytes never go into the
 * database. Implementations live in {@code platform-app} (devops): a local volume now
 * ({@code app.files.store=local}, {@code app.files.local-root}), an S3-compatible bucket later
 * ({@code app.files.store=s3}), both encrypted at rest. Keys are built by the caller, e.g.
 * {@code person/<personId>/<fileId>} or {@code intake/<itemId>}, and never contain names.
 *
 * <p>Writes are not transactional: callers write the bytes before the database row commits and
 * delete bytes only after the commit that removed the row (a nightly sweep removes orphans).
 */
public interface FileStore {

    /**
     * Stores the bytes under the key, replacing any existing object.
     *
     * @param key         opaque key, {@code [a-z0-9/-]}, at most 300 characters
     * @param content     the bytes; the caller closes the stream
     * @param size        number of bytes
     * @param contentType detected media type, e.g. {@code application/pdf}
     * @throws IOException when the store cannot write
     */
    void put(String key, InputStream content, long size, String contentType) throws IOException;

    /**
     * Opens the stored bytes.
     *
     * @param key the key given to {@link #put}
     * @return the content, which the caller must close, or empty when there is no such object
     * @throws IOException when the store cannot read
     */
    Optional<InputStream> get(String key) throws IOException;

    /**
     * Deletes the object; deleting a missing key is not an error (idempotent).
     *
     * @param key the key given to {@link #put}
     * @throws IOException when the store cannot delete
     */
    void delete(String key) throws IOException;
}
