package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.accounts.account.web.dto.AccountCreateRequest;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.importing.app.ImportAnalysis.Row;
import com.recruiting.platform.accounts.importing.parse.TableReader;
import com.recruiting.platform.accounts.importing.parse.TableReader.Table;
import com.recruiting.platform.accounts.importing.web.dto.ImportRowError;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.PersonType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Dry-run validation of an import file (A-11). Nothing is written. Rows match existing records by e-mail
 * (people, contacts) or exact name (accounts) and are counted as duplicates; so are repeats inside the file.
 */
@Component
public class ImportAnalyzer {

    public static final int MAX_ROWS = 10_000;

    private final AccountRepository accounts;
    private final PersonRepository people;
    private final UserDirectory users;
    private final Validator validator;

    public ImportAnalyzer(AccountRepository accounts, PersonRepository people, UserDirectory users,
            Validator validator) {
        this.accounts = accounts;
        this.people = people;
        this.users = users;
        this.validator = validator;
    }

    /** @throws IllegalArgumentException when the file is unreadable, has missing columns or too many rows */
    public ImportAnalysis analyze(ImportKind kind, String fileName, byte[] content) {
        Table table = TableReader.read(fileName, content);
        for (String required : ImportTemplates.required(kind)) {
            if (!table.headers().contains(required.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Missing column: " + required);
            }
        }
        if (table.rows().size() > MAX_ROWS) {
            throw new IllegalArgumentException("More than " + MAX_ROWS + " rows.");
        }
        List<ImportRowError> errors = new ArrayList<>();
        List<Row> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int errorRows = 0;
        int duplicates = 0;
        int n = 0;
        for (List<String> cells : table.rows()) {
            n++;
            List<ImportRowError> rowErrors = new ArrayList<>();
            Cells row = new Cells(table.headers(), cells);
            Row parsed = switch (kind) {
                case ACCOUNTS -> account(row, n, rowErrors, seen);
                case CONTACTS -> contact(row, n, rowErrors, seen);
                case PEOPLE -> person(row, n, rowErrors, seen);
            };
            if (!rowErrors.isEmpty()) {
                errorRows++;
                rowErrors.stream().limit(Math.max(0, ImportAnalysis.MAX_ERRORS - errors.size())).forEach(errors::add);
            } else if (parsed == null) {
                duplicates++;
            } else {
                valid.add(parsed);
            }
        }
        return new ImportAnalysis(table.rows().size(), errorRows, duplicates, errors, valid);
    }

    private Row account(Cells c, int n, List<ImportRowError> errs, Set<String> seen) {
        UUID owner = owner(c, n, errs);
        AccountCreateRequest r = new AccountCreateRequest(c.get("name"), c.get("sector"), c.get("city"),
                c.get("website"), null, owner);
        violations(r, n, errs);
        if (!errs.isEmpty()) {
            return null;
        }
        String key = "a:" + r.name().toLowerCase(Locale.ROOT);
        if (!seen.add(key) || accounts.findByNameIgnoreCase(r.name().trim()).isPresent()) {
            return null;
        }
        return new Row(r, null, null, null);
    }

    private Row person(Cells c, int n, List<ImportRowError> errs, Set<String> seen) {
        UUID owner = owner(c, n, errs);
        PersonType type = type(c, n, errs);
        PersonCreateRequest r = new PersonCreateRequest(c.get("name"), type, c.get("title"), c.get("company"),
                c.get("email"), c.get("phone"), c.get("city"), null, null, null, owner, null);
        return personRow(r, null, null, n, errs, seen);
    }

    private Row contact(Cells c, int n, List<ImportRowError> errs, Set<String> seen) {
        var account = c.get("accountName") == null ? java.util.Optional.<com.recruiting.platform.accounts.domain.Account>empty()
                : accounts.findByNameIgnoreCase(c.get("accountName"));
        if (account.isEmpty()) {
            errs.add(new ImportRowError(n, "accountName", "No account with this name"));
        }
        PersonCreateRequest r = new PersonCreateRequest(c.get("name"), PersonType.CLIENT_CONTACT, c.get("title"),
                null, c.get("email"), c.get("phone"), c.get("city"), null, null, null, null, null);
        return personRow(r, account.map(a -> a.getId()).orElse(null), c.get("role"), n, errs, seen);
    }

    private Row personRow(PersonCreateRequest r, UUID accountId, String role, int n, List<ImportRowError> errs,
            Set<String> seen) {
        violations(r, n, errs);
        if (!errs.isEmpty()) {
            return null;
        }
        if (r.email() != null) {
            String email = r.email().toLowerCase(Locale.ROOT);
            if (!seen.add("p:" + email) || people.findByEmailIgnoreCase(email).isPresent()) {
                return null;
            }
        }
        return new Row(null, r, accountId, role);
    }

    private UUID owner(Cells c, int n, List<ImportRowError> errs) {
        String email = c.get("ownerEmail");
        if (email == null) {
            return null;
        }
        return users.activeIdByEmail(email).orElseGet(() -> {
            errs.add(new ImportRowError(n, "ownerEmail", "No active user with this e-mail address"));
            return null;
        });
    }

    private PersonType type(Cells c, int n, List<ImportRowError> errs) {
        String raw = c.get("type");
        if (raw == null) {
            return null;
        }
        try {
            return PersonType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errs.add(new ImportRowError(n, "type", "Must be one of EXECUTIVE, CLIENT_CONTACT, REFERRER, CANDIDATE"));
            return null;
        }
    }

    private <T> void violations(T request, int n, List<ImportRowError> errs) {
        for (ConstraintViolation<T> v : validator.validate(request)) {
            String column = v.getPropertyPath().toString();
            if (errs.stream().noneMatch(e -> column.equals(e.column()))) {
                errs.add(new ImportRowError(n, column, v.getMessage()));
            }
        }
    }

    /** One row looked up by header name; blank cells read as null. */
    private record Cells(List<String> headers, List<String> values) {

        String get(String column) {
            int i = headers.indexOf(column.toLowerCase(Locale.ROOT));
            if (i < 0 || i >= values.size() || values.get(i).isBlank()) {
                return null;
            }
            return values.get(i).trim();
        }
    }
}
