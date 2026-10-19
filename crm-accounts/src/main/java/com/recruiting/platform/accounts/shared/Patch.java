package com.recruiting.platform.accounts.shared;

import java.util.Set;

/** A PATCH body plus the names of the JSON fields that were present (absent = unchanged, null = clear). */
public record Patch<T>(T body, Set<String> present) {

    public boolean has(String field) {
        return present.contains(field);
    }
}
