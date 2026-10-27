package com.recruiting.platform.accounts.person.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recruiting.platform.accounts.domain.CvIntakeItemRepository;
import com.recruiting.platform.accounts.domain.DuplicateCandidateRepository;
import com.recruiting.platform.accounts.domain.NetworkLinkRepository;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PersonDataServiceTest {

    @Test
    void anonymiseClearsTags() {
        PersonRepository people = mock(PersonRepository.class);
        NetworkLinkRepository links = mock(NetworkLinkRepository.class);
        PersonDataService service = new PersonDataService(people, mock(PersonEmploymentRepository.class), links,
                mock(PersonEducationRepository.class), mock(PersonFileRepository.class),
                mock(CvIntakeItemRepository.class), mock(DuplicateCandidateRepository.class),
                mock(FileStore.class), mock(ApplicationEventPublisher.class));
        Person p = new Person();
        p.setName("Ayse Demir");
        p.setTags(List.of("cfo-network", "vip"));
        p.setNoticePeriodDays((short) 30);
        p.setPhoneNormalized("+905550000000");
        p.setLanguages(List.of(java.util.Map.of("language", "en", "level", "FLUENT")));
        UUID id = UUID.randomUUID();
        when(people.findById(id)).thenReturn(Optional.of(p));
        when(people.saveAndFlush(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        when(links.findByFromPersonIdOrToPersonId(id, id)).thenReturn(List.of());

        service.anonymise(id, UUID.randomUUID());

        assertThat(p.getTags()).isEmpty();
        assertThat(p.getNoticePeriodDays()).isNull();
        assertThat(p.getPhoneNormalized()).isNull();
        assertThat(p.getLanguages()).isEmpty();
        assertThat(p.getName()).isEqualTo("Anonymised person");
        assertThat(p.getAnonymisedAt()).isNotNull();
    }
}
