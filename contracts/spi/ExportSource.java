package com.recruiting.platform.contracts;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes the rows of one list entity for {@code POST /exports} (CO-013, since contract 1.3.0,
 * ADR 0013). {@code security} owns the export job, approval, watermark and audit; it finds the
 * bean whose {@link #entity()} matches. {@code accounts} implements PEOPLE and ACCOUNTS,
 * {@code pipeline} JOB_ORDERS (PROPOSALS is cut from 1.3: the caller rejects it with 400). Filters, columns and sort are already validated
 * against the entity's {@code ViewDefinition} rules by the caller.
 */
public interface ExportSource {

    /**
     * The entity this source exports.
     *
     * @return the list entity
     */
    ListEntity entity();

    /**
     * Counts the rows the filters match (for the approval threshold).
     *
     * @param filters view filters, keys and values as in {@code ViewDefinition.filters}
     * @return number of rows
     */
    long count(Map<String, Object> filters);

    /**
     * Writes the CSV body (header row plus data rows, no watermark) for the filters.
     *
     * @param filters             view filters
     * @param columns             columns in order
     * @param sort                sort keys, {@code property,asc|desc}
     * @param includeCompensation PEOPLE only: add decrypted compensation columns (MANAGER/ADMIN,
     *                            checked by the caller)
     * @param out                 the target; the caller closes it
     * @return the ids of the exported records (PEOPLE: for one EXPORT access-log row each)
     * @throws IOException when writing fails
     */
    List<UUID> write(Map<String, Object> filters, List<String> columns, List<String> sort,
                     boolean includeCompensation, Writer out) throws IOException;
}
