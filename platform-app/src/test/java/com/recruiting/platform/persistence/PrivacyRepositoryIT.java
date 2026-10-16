package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.privacy.domain.ConsentRecord;
import com.recruiting.platform.privacy.domain.ConsentRecordRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacy;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.domain.RetentionSettingsRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PrivacyRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private PersonRepository people;

    @Autowired
    private PersonPrivacyRepository personPrivacy;

    @Autowired
    private PrivacyNoticeRepository notices;

    @Autowired
    private ConsentRecordRepository consentRecords;

    @Autowired
    private RetentionSettingsRepository settings;

    @Autowired
    private EntityManager em;

    private Person newPerson() {
        Person p = new Person();
        p.setName("Privacy Person " + UUID.randomUUID());
        p.setType(PersonType.EXECUTIVE);
        p.setOwnerId(newUser().getId());
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> people.save(p));
    }

    private PersonPrivacy privacyFor(Person p) {
        PersonPrivacy pp = new PersonPrivacy(p.getId());
        pp.setConsentStatus(ConsentStatus.NOT_SENT);
        pp.setBasisDate(LocalDate.now());
        pp.setRetainUntil(LocalDate.now().plusMonths(24));
        pp.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> personPrivacy.save(pp));
    }

    @Test
    void settingsRowIsSeededAndUpdatable() {
        RetentionSettings s = settings.findById((short) 1).orElseThrow();
        assertThat(s.getRetentionMonths()).isEqualTo(24);
        assertThat(s.getAction()).isEqualTo(RetentionAction.ANONYMISE);
        assertThat(s.getAutoSendPersonTypes()).isEqualTo("CANDIDATE,EXECUTIVE");

        long before = s.getVersion();
        tx.executeWithoutResult(t -> settings.findById((short) 1).orElseThrow().setWarningDays(31));
        RetentionSettings updated = settings.findById((short) 1).orElseThrow();
        assertThat(updated.getWarningDays()).isEqualTo(31);
        assertThat(updated.getVersion()).isEqualTo(before + 1);
        tx.executeWithoutResult(t -> settings.findById((short) 1).orElseThrow().setWarningDays(30));
    }

    @Test
    void currentNoticeIsTheLatestEffectiveOne() {
        String v = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 2; i++) {
            PrivacyNotice n = new PrivacyNotice();
            n.setVersion(v + "-" + i);
            n.setLanguage(Language.EN);
            n.setNoticeText("notice " + i);
            n.setConsentText("consent " + i);
            n.setEffectiveFrom(LocalDate.of(2030, 1, 1).plusDays(i));
            tx.executeWithoutResult(s -> notices.save(n));
        }
        var current = notices.findFirstByLanguageAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                Language.EN, LocalDate.of(2030, 1, 2));
        assertThat(current).isPresent();
        assertThat(current.get().getVersion()).isEqualTo(v + "-1");
        assertThat(current.get().getCreatedAt()).isNotNull();
    }

    @Test
    void consentHistoryIsKeptNewestFirstAndDeletedWithThePerson() {
        Person p = newPerson();
        privacyFor(p);
        PrivacyNotice n = new PrivacyNotice();
        n.setVersion(UUID.randomUUID().toString().substring(0, 8));
        n.setLanguage(Language.TR);
        n.setNoticeText("a");
        n.setConsentText("b");
        n.setEffectiveFrom(LocalDate.now());
        tx.executeWithoutResult(s -> notices.save(n));
        for (ConsentDecision d : List.of(ConsentDecision.GIVEN, ConsentDecision.WITHDRAWN)) {
            ConsentRecord r = new ConsentRecord();
            r.setPersonId(p.getId());
            r.setDecision(d);
            r.setChannel(ConsentChannel.IN_PERSON);
            r.setNoticeId(n.getId());
            r.setRecordedAt(Instant.now().plusSeconds(d == ConsentDecision.WITHDRAWN ? 10 : 0));
            tx.executeWithoutResult(s -> consentRecords.save(r));
        }
        assertThat(consentRecords.findByPersonIdOrderByRecordedAtDesc(p.getId()))
                .extracting(ConsentRecord::getDecision)
                .containsExactly(ConsentDecision.WITHDRAWN, ConsentDecision.GIVEN);

        // DELETE retention path: the person row goes, dependent privacy rows cascade
        tx.executeWithoutResult(s -> people.deleteById(p.getId()));
        assertThat(consentRecords.findByPersonIdOrderByRecordedAtDesc(p.getId())).isEmpty();
        assertThat(personPrivacy.findById(p.getId())).isEmpty();
    }

    @Test
    void enversRecordsPersonPrivacyChanges() {
        Person p = newPerson();
        privacyFor(p);
        tx.executeWithoutResult(s -> {
            PersonPrivacy pp = personPrivacy.findById(p.getId()).orElseThrow();
            pp.setConsentStatus(ConsentStatus.GIVEN);
        });
        tx.executeWithoutResult(s -> {
            var reader = AuditReaderFactory.get(em);
            List<Number> revs = reader.getRevisions(PersonPrivacy.class, p.getId());
            assertThat(revs).hasSize(2);
            assertThat(reader.find(PersonPrivacy.class, p.getId(), revs.get(1)).getConsentStatus())
                    .isEqualTo(ConsentStatus.GIVEN);
        });
    }
}
