package com.recruiting.platform.accounts.contact.web;

import com.recruiting.platform.accounts.contact.app.ContactService;
import com.recruiting.platform.accounts.contact.web.dto.ContactResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** /people/{id}/contact-roles: the accounts where a person is or was a contact. */
@RestController
@RequestMapping("/api/v1/people/{id}/contact-roles")
@PreAuthorize("hasRole('CONSULTANT')")
public class PersonContactRolesController {

    private final ContactService service;

    public PersonContactRolesController(ContactService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContactResponse> list(@PathVariable UUID id) {
        return service.listForPerson(id);
    }
}
