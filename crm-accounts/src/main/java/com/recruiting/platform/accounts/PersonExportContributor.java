package com.recruiting.platform.accounts;

import java.util.UUID;

/**
 * SPI for GET /people/{id}/export (S-04): the module that owns a part of the PersonDataExport implements this and
 * accounts adds it to the document. Sections: {@code activities} (list of Activity) and {@code applications}
 * (list of Application) from pipeline, {@code consent} (ConsentOverview) from privacy. A section nobody
 * contributes is empty list for the two lists and left out for {@code consent}. Values must be the API
 * representation (openapi schemas); they are written as they are.
 */
public interface PersonExportContributor {

    /** One of {@code activities}, {@code applications}, {@code consent}. */
    String section();

    /** The JSON-ready value of the section for this person. */
    Object collect(UUID personId);
}
