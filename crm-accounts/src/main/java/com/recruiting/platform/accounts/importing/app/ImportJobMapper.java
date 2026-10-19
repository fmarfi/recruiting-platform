package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.accounts.domain.ImportJob;
import com.recruiting.platform.accounts.importing.web.dto.ImportJobResponse;
import com.recruiting.platform.accounts.importing.web.dto.ImportRowError;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ImportJob entity to the ImportJob response; the error list is stored as JSON maps. */
@Component
public class ImportJobMapper {

    private final UserDirectory users;

    public ImportJobMapper(UserDirectory users) {
        this.users = users;
    }

    public ImportJobResponse response(ImportJob j) {
        return build(j, users.refs(java.util.Collections.singletonList(j.getCreatedBy())));
    }

    public List<ImportJobResponse> responses(Collection<ImportJob> jobs) {
        Map<UUID, UserRef> refs = users.refs(jobs.stream().map(ImportJob::getCreatedBy).toList());
        return jobs.stream().map(j -> build(j, refs)).toList();
    }

    static Map<String, Object> toMap(ImportRowError e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("row", e.row());
        m.put("column", e.column());
        m.put("message", e.message());
        return m;
    }

    private static ImportJobResponse build(ImportJob j, Map<UUID, UserRef> refs) {
        List<ImportRowError> errors = j.getErrors() == null ? List.of()
                : j.getErrors().stream().map(m -> new ImportRowError(((Number) m.get("row")).intValue(),
                        (String) m.get("column"), (String) m.get("message"))).toList();
        return new ImportJobResponse(j.getId(), j.getKind(), j.getStatus(), j.getFileName(), n(j.getTotalRows()),
                n(j.getValidRows()), n(j.getDuplicateRows()), n(j.getImportedRows()), errors, j.getFailureReason(),
                j.getCreatedAt(), refs.get(j.getCreatedBy()), j.getCompletedAt());
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }
}
