package com.recruiting.platform.accounts.contact.app;

import com.recruiting.platform.accounts.contact.web.dto.ContactResponse;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Contact;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.dto.AccountRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Contact entity to response record, loading accounts and people for a whole list at once. */
@Component
public class ContactMapper {

    private final AccountRepository accounts;
    private final PersonRepository people;
    private final PersonMapper personMapper;

    public ContactMapper(AccountRepository accounts, PersonRepository people, PersonMapper personMapper) {
        this.accounts = accounts;
        this.people = people;
        this.personMapper = personMapper;
    }

    public List<ContactResponse> responses(Collection<Contact> contacts) {
        Map<UUID, Account> accountById = accounts
                .findAllByIdIn(contacts.stream().map(Contact::getAccountId).distinct().toList()).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        List<Person> persons = people.findAllByIdIn(contacts.stream().map(Contact::getPersonId).distinct().toList());
        Map<UUID, PersonSummaryResponse> summaries = personMapper.summaries(persons).stream()
                .collect(Collectors.toMap(PersonSummaryResponse::id, Function.identity()));
        return contacts.stream().map(c -> {
            Account a = accountById.get(c.getAccountId());
            return new ContactResponse(c.getId(), new AccountRef(a.getId(), a.getName(), a.getStatus()),
                    summaries.get(c.getPersonId()), c.getRole(), c.isPrimary(), c.getEndedOn(),
                    c.getEndedOn() == null, c.getCreatedAt());
        }).toList();
    }

    public ContactResponse response(Contact c) {
        return responses(List.of(c)).get(0);
    }
}
