package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.contracts.ImportKind;
import java.util.List;

/** Expected columns per import kind (A-11); the first columns listed as required must be present in the file. */
public final class ImportTemplates {

    private ImportTemplates() {
    }

    public static List<String> columns(ImportKind kind) {
        return switch (kind) {
            case ACCOUNTS -> List.of("name", "sector", "city", "website", "ownerEmail");
            case CONTACTS -> List.of("accountName", "name", "title", "email", "phone", "city", "role");
            case PEOPLE -> List.of("name", "type", "title", "company", "email", "phone", "city", "ownerEmail");
        };
    }

    public static List<String> required(ImportKind kind) {
        return switch (kind) {
            case ACCOUNTS -> List.of("name", "sector");
            case CONTACTS -> List.of("accountName", "name");
            case PEOPLE -> List.of("name", "type");
        };
    }

    /** Header row only, with a trailing newline. */
    public static String csv(ImportKind kind) {
        return String.join(",", columns(kind)) + "\n";
    }
}
