package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.PersonExportContributor;
import com.recruiting.platform.accounts.contact.app.ContactService;
import com.recruiting.platform.accounts.networklink.app.ConnectionQueryService;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.security.AccessRecorder;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.UserDirectory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the PersonDataExport (S-04, NFR-11): person, contact roles and links from accounts, the rest from
 * {@link PersonExportContributor}s. Writes the EXPORT access-log row in the same transaction.
 */
@Service
@Transactional
public class PersonExportService {

    private final PersonService people;
    private final ContactService contacts;
    private final ConnectionQueryService connections;
    private final List<PersonExportContributor> contributors;
    private final UserDirectory users;
    private final CurrentUser currentUser;
    private final AccessRecorder access;

    public PersonExportService(PersonService people, ContactService contacts, ConnectionQueryService connections,
            List<PersonExportContributor> contributors, UserDirectory users, CurrentUser currentUser,
            AccessRecorder access) {
        this.people = people;
        this.contacts = contacts;
        this.connections = connections;
        this.contributors = contributors;
        this.users = users;
        this.currentUser = currentUser;
        this.access = access;
    }

    public Map<String, Object> export(UUID personId, String ipAddress) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("exportedAt", Instant.now());
        doc.put("exportedBy", users.ref(currentUser.id()).orElse(null));
        doc.put("person", people.get(personId));
        doc.put("contactRoles", contacts.listForPerson(personId));
        doc.put("links", connections.forPerson(personId));
        doc.put("activities", List.of());
        doc.put("applications", List.of());
        for (PersonExportContributor c : contributors) {
            doc.put(c.section(), c.collect(personId));
        }
        access.record(AccessAction.EXPORT, EntityType.PERSON, personId, ipAddress);
        return doc;
    }
}
