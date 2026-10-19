package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.person.web.dto.Employment;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.UserLookup;
import com.recruiting.platform.accounts.shared.dto.UserRef;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Entity to response records for people. Owners are looked up in one query per list. */
@Component
public class PersonMapper {

    private final UserLookup users;
    private final PersonEmploymentRepository employments;

    public PersonMapper(UserLookup users, PersonEmploymentRepository employments) {
        this.users = users;
        this.employments = employments;
    }

    public List<PersonSummaryResponse> summaries(Collection<Person> people) {
        Map<UUID, UserRef> owners = users.refs(people.stream().map(Person::getOwnerId).toList());
        return people.stream().map(p -> summary(p, owners)).toList();
    }

    public PersonSummaryResponse summary(Person p) {
        return summary(p, users.refs(List.of(p.getOwnerId())));
    }

    private PersonSummaryResponse summary(Person p, Map<UUID, UserRef> owners) {
        return new PersonSummaryResponse(p.getId(), p.getName(), p.getTitle(), p.getCompany(), p.getType(),
                p.getCity(), p.getEmail(), owners.get(p.getOwnerId()), p.getLastContactAt(),
                p.getConsentStatus(), p.getRetentionStatus());
    }

    public PersonResponse response(Person p) {
        PersonSummaryResponse s = summary(p);
        List<Employment> history = employments.findByPersonIdOrderBySortOrder(p.getId()).stream()
                .map(PersonMapper::employment).toList();
        return new PersonResponse(s.id(), s.name(), s.title(), s.company(), s.type(), s.city(), s.email(),
                s.owner(), s.lastContactAt(), s.consentStatus(), s.retentionStatus(), p.getPhone(),
                p.getLinkedinUrl(), p.getHowWeKnow(), p.getNotes(), history, p.getAnonymisedAt() != null,
                p.getCreatedAt(), p.getUpdatedAt(), p.getVersion() == null ? 0 : p.getVersion());
    }

    private static Employment employment(PersonEmployment e) {
        return new Employment(e.getCompany(), e.getTitle(), e.getStartDate(), e.getEndDate());
    }
}
