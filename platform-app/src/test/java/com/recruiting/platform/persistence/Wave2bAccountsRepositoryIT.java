package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.accounts.domain.CvIntakeItem;
import com.recruiting.platform.accounts.domain.CvIntakeItemRepository;
import com.recruiting.platform.accounts.domain.DuplicateCandidate;
import com.recruiting.platform.accounts.domain.DuplicateCandidateRepository;
import com.recruiting.platform.accounts.domain.OffLimitsRule;
import com.recruiting.platform.accounts.domain.OffLimitsRuleRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEducation;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.domain.PersonMerge;
import com.recruiting.platform.accounts.domain.PersonMergeRepository;
import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.DuplicateStatus;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.OffLimitsReason;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.contracts.TextExtractionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Wave 2b tables owned by accounts (contract 1.2.0, ADR 0011) and the 1.1.1 city index. */
class Wave2bAccountsRepositoryIT extends Wave2bSupport {

    @Autowired
    private PersonEducationRepository educations;

    @Autowired
    private PersonFileRepository files;

    @Autowired
    private CvIntakeItemRepository intake;

    @Autowired
    private DuplicateCandidateRepository duplicates;

    @Autowired
    private PersonMergeRepository merges;

    @Autowired
    private OffLimitsRuleRepository offLimits;

    // ---------------------------------------------------------------- person profile

    @Test
    void personProfileColumnsRoundTripAndAreAudited() {
        UUID owner = newUser().getId();
        Person p = new Person();
        p.setName(unique("Profile"));
        p.setOwnerId(owner);
        p.setType(com.recruiting.platform.contracts.PersonType.CANDIDATE);
        p.setSourceChannel(SourceChannel.CV_UPLOAD);
        p.setNoticePeriodDays((short) 90);
        p.setMobility(Mobility.WITHIN_COUNTRY);
        p.setCompensationEnc(new byte[] {1, 2, 3, 4, 5});
        p.setLanguages(List.of(Map.of("language", "EN", "level", "FLUENT")));
        p.setPhoneNormalized("+905551112233");
        p.setLinkedinUrlNormalized("https://www.linkedin.com/in/fictional-" + UUID.randomUUID());
        UUID id = tx.execute(s -> people.save(p)).getId();

        Person loaded = people.findById(id).orElseThrow();
        assertThat(loaded.getSourceChannel()).isEqualTo(SourceChannel.CV_UPLOAD);
        assertThat(loaded.getNoticePeriodDays()).isEqualTo((short) 90);
        assertThat(loaded.getMobility()).isEqualTo(Mobility.WITHIN_COUNTRY);
        assertThat(loaded.getCompensationEnc()).containsExactly(1, 2, 3, 4, 5);
        assertThat(loaded.getLanguages()).singleElement().satisfies(l -> assertThat(l).containsEntry("level", "FLUENT"));
        assertThat(people.findByPhoneNormalized("+905551112233")).extracting(Person::getId).contains(id);
        assertThat(people.findByLinkedinUrlNormalized(p.getLinkedinUrlNormalized())).hasSize(1);
        // Envers copied the new columns (the ciphertext is audited as is).
        assertThat(count("SELECT count(*) FROM person_aud WHERE id = ? AND source_channel = 'CV_UPLOAD' "
                + "AND notice_period_days = 90 AND compensation_enc IS NOT NULL AND jsonb_array_length(languages) = 1",
                id)).isOne();
    }

    @Test
    void personProfileChecksAreEnforced() {
        UUID owner = newUser().getId();
        UUID id = person(owner);
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET notice_period_days = 366 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET notice_period_days = -1 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET mobility = 'MOON' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET source_channel = 'CARRIER_PIGEON' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET languages = '{}'::jsonb WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(people.findById(id).orElseThrow().getLanguages()).isEmpty();
    }

    @Test
    void cityAndNameCompanyLookupsIgnoreCaseAndAccents() {
        UUID owner = newUser().getId();
        String city = unique("Çanakkale");
        String name = unique("Öztürk");
        Person p = new Person();
        p.setName(name);
        p.setCompany("Şirket A.Ş.");
        p.setCity(city);
        p.setOwnerId(owner);
        p.setType(com.recruiting.platform.contracts.PersonType.CANDIDATE);
        UUID id = tx.execute(s -> people.save(p)).getId();

        assertThat(people.findByCityNormalized(" " + city.toUpperCase().replace("Ç", "c") + " "))
                .extracting(Person::getId).contains(id);
        assertThat(people.findByNameAndCompanyNormalized(name.toLowerCase().replace("ö", "o"), "SIRKET a.s."))
                .extracting(Person::getId).containsExactly(id);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname IN ('idx_person_city', 'idx_person_name_company_norm')",
                Integer.class)).isEqualTo(2);
    }

    // ---------------------------------------------------------------- person_education

    @Test
    void educationRoundTripYearRuleAndCascade() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        PersonEducation e = education(personId, 2008, 2012, 0);
        tx.execute(s -> educations.save(education(personId, null, 2016, 1)));
        tx.execute(s -> educations.save(e));
        assertThat(educations.findByPersonIdOrderBySortOrder(personId)).hasSize(2)
                .first().satisfies(x -> assertThat(x.getSchool()).isEqualTo("Fictional University"));
        assertThat(count("SELECT count(*) FROM person_education_aud WHERE person_id = ?", personId)).isEqualTo(2);

        assertThatThrownBy(() -> educations.saveAndFlush(education(personId, 2012, 2008, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);

        tx.executeWithoutResult(s -> people.deleteById(personId));
        assertThat(count("SELECT count(*) FROM person_education WHERE person_id = ?", personId)).isZero();
    }

    private PersonEducation education(UUID personId, Integer start, Integer end, int order) {
        PersonEducation e = new PersonEducation();
        e.setPersonId(personId);
        e.setSchool("Fictional University");
        e.setDegree("BSc");
        e.setStartYear(start == null ? null : start.shortValue());
        e.setEndYear(end == null ? null : end.shortValue());
        e.setSortOrder(order);
        return e;
    }

    // ---------------------------------------------------------------- person_file

    private PersonFile file(UUID personId, UUID uploader, PersonFileKind kind, int version, boolean current,
            char shaChar, String text) {
        PersonFile f = new PersonFile();
        f.setPersonId(personId);
        f.setKind(kind);
        f.setVersion(version);
        f.setCurrent(current);
        f.setFileName("cv-" + shaChar + ".pdf");
        f.setContentType("application/pdf");
        f.setSizeBytes(1234L);
        f.setSha256(sha(shaChar));
        f.setStorageKey("person/" + personId + "/" + UUID.randomUUID());
        f.setUploadedBy(uploader);
        f.setExtractedText(text);
        f.setTextStatus(text == null ? TextExtractionStatus.PENDING : TextExtractionStatus.DONE);
        return tx.execute(s -> files.saveAndFlush(f));
    }

    @Test
    void personFileRoundTripAuditWithoutExtractedTextAndRowVersion() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        PersonFile f = file(personId, owner, PersonFileKind.CV, 1, true, 'a', "Fictional CV text");

        PersonFile loaded = files.findById(f.getId()).orElseThrow();
        assertThat(loaded.getVersion()).isEqualTo(1);
        assertThat(loaded.getRowVersion()).isNotNull();
        assertThat(loaded.getExtractedText()).isEqualTo("Fictional CV text");
        assertThat(files.findByPersonIdAndKindAndCurrentTrue(personId, PersonFileKind.CV)).isPresent();
        assertThat(files.findByPersonIdAndSha256(personId, sha('a'))).isPresent();
        assertThat(files.maxVersion(personId, PersonFileKind.CV)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM person_file_aud WHERE id = ? AND revtype = 0 AND version = 1",
                f.getId())).isOne();
        // extracted_text and text_tsv are not audited
        assertThat(count("SELECT count(*) FROM information_schema.columns WHERE table_name = 'person_file_aud' "
                + "AND column_name IN ('extracted_text', 'text_tsv')")).isZero();
        assertThat(files.findPendingText(1000)).extracting(PersonFile::getId).doesNotContain(f.getId());
    }

    @Test
    void personFileConstraints() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        file(personId, owner, PersonFileKind.CV, 1, true, 'a', null);
        // same bytes twice for one person
        assertThatThrownBy(() -> file(personId, owner, PersonFileKind.CV, 2, false, 'a', null))
                .isInstanceOf(DataIntegrityViolationException.class);
        // second current CV
        assertThatThrownBy(() -> file(personId, owner, PersonFileKind.CV, 2, true, 'b', null))
                .isInstanceOf(DataIntegrityViolationException.class);
        // same CV version number
        assertThatThrownBy(() -> file(personId, owner, PersonFileKind.CV, 1, false, 'c', null))
                .isInstanceOf(DataIntegrityViolationException.class);
        // an older CV that is not current is fine, so is a document, and the same bytes for another person
        file(personId, owner, PersonFileKind.CV, 2, false, 'd', null);
        file(personId, owner, PersonFileKind.DOCUMENT, 1, true, 'e', null);
        file(personId, owner, PersonFileKind.DOCUMENT, 1, true, 'f', null);
        file(person(owner), owner, PersonFileKind.CV, 1, true, 'a', null);
        // size bounds
        assertThatThrownBy(() -> jdbc.update("UPDATE person_file SET size_bytes = 0 WHERE person_id = ?", personId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person_file SET size_bytes = 10485761 WHERE person_id = ?",
                personId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE person_file SET kind = 'PHOTO' WHERE person_id = ?", personId))
                .isInstanceOf(DataIntegrityViolationException.class);
        // storage key is unique
        String key = jdbc.queryForObject("SELECT storage_key FROM person_file WHERE person_id = ? LIMIT 1",
                String.class, personId);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE person_file SET storage_key = ? WHERE person_id = ? AND storage_key <> ?",
                key, personId, key)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cvTextSearchIsTurkishAccentInsensitiveAndOnlyCoversTheCurrentCv() {
        UUID owner = newUser().getId();
        String token = "zxq" + Long.toString(System.nanoTime(), 36);
        UUID current = person(owner);
        UUID other = person(owner);
        UUID old = person(owner);
        UUID doc = person(owner);
        file(current, owner, PersonFileKind.CV, 1, true, 'a',
                "Şişli'de çalışan kıdemli MÜHENDİSLER ve Öğretmen " + token);
        file(other, owner, PersonFileKind.CV, 1, true, 'g', "İstanbul'da " + token);
        file(old, owner, PersonFileKind.CV, 1, false, 'b', "Çalışkan " + token);
        file(doc, owner, PersonFileKind.DOCUMENT, 1, true, 'c', "Çalışkan " + token);

        // accents and case do not matter, in both directions
        assertThat(files.findPersonIdsByCvText("sisli " + token)).containsExactly(current);
        assertThat(files.findPersonIdsByCvText("ŞİŞLİ")).contains(current);
        assertThat(files.findPersonIdsByCvText("ogretmen")).contains(current);
        // Turkish dotless i and dotted I, both directions
        assertThat(files.findPersonIdsByCvText("kidemli " + token)).containsExactly(current);
        assertThat(files.findPersonIdsByCvText("KIDEMLI " + token)).containsExactly(current);
        assertThat(files.findPersonIdsByCvText("kıdemli " + token)).containsExactly(current);
        assertThat(files.findPersonIdsByCvText("İSTANBUL " + token)).containsExactly(other);
        // Turkish stemming: a plural matches the singular
        assertThat(files.findPersonIdsByCvText("mühendis " + token)).containsExactly(current);
        // old CV versions and non-CV documents are not searched
        assertThat(files.findPersonIdsByCvText(token)).containsExactlyInAnyOrder(current, other);
        assertThat(jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_person_file_tsv'", String.class))
                .contains("USING gin").contains("text_tsv");
    }

    @Test
    void deletingAPersonCascadesToFilesAndKeepsNothingBehind() {
        UUID owner = newUser().getId();
        UUID personId = person(owner);
        PersonFile f = file(personId, owner, PersonFileKind.CV, 1, true, 'a', "text");
        tx.executeWithoutResult(s -> people.deleteById(personId));
        assertThat(files.findById(f.getId())).isEmpty();
        // the audit history stays until the privacy purge removes it (ADR 0009 step 4)
        assertThat(count("SELECT count(*) FROM person_file_aud WHERE id = ?", f.getId())).isPositive();
    }

    // ---------------------------------------------------------------- cv_intake_item

    private CvIntakeItem intakeItem(UUID uploader, String... emails) {
        CvIntakeItem i = new CvIntakeItem();
        i.setBatchId(UUID.randomUUID());
        i.setFileName("incoming.pdf");
        i.setContentType("application/pdf");
        i.setSizeBytes(100L);
        i.setSha256(sha('9'));
        i.setStorageKey("intake/" + UUID.randomUUID());
        i.setUploadedBy(uploader);
        i.setUploadedAt(Instant.now());
        i.setExpiresAt(Instant.now().plusSeconds(14 * 86400L));
        i.setSourceChannel(SourceChannel.CV_UPLOAD);
        i.setDetected(Map.of("emails", List.of(emails), "phones", List.of(), "linkedinUrls", List.of()));
        return tx.execute(s -> intake.saveAndFlush(i));
    }

    @Test
    void intakeItemDefaultsDetectedLookupAndStatusFlow() {
        UUID uploader = newUser().getId();
        String email = unique("candidate") + "@example.test";
        CvIntakeItem i = intakeItem(uploader, email, "other@example.test");
        CvIntakeItem loaded = intake.findById(i.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(CvIntakeStatus.PENDING);
        assertThat(loaded.getDetected().get("emails")).contains(email);
        assertThat(intake.findPendingByDetectedEmail(email)).extracting(CvIntakeItem::getId).containsExactly(i.getId());
        assertThat(intake.findPendingByDetectedEmail("nobody@example.test")).isEmpty();
        assertThat(intake.findByBatchId(i.getBatchId())).hasSize(1);
        assertThat(intake.findByStatusAndExpiresAtBefore(CvIntakeStatus.PENDING, Instant.now().plusSeconds(15 * 86400L)))
                .extracting(CvIntakeItem::getId).contains(i.getId());

        // minimised on resolve: the item no longer matches
        loaded.setStatus(CvIntakeStatus.DISCARDED);
        loaded.setFileName(null);
        loaded.setStorageKey(null);
        loaded.setExtractedText(null);
        loaded.setDetected(CvIntakeItem.emptyDetected());
        tx.execute(s -> intake.saveAndFlush(loaded));
        assertThat(intake.findPendingByDetectedEmail(email)).isEmpty();

        // column default for detected when the application does not set it
        UUID rawId = UUID.randomUUID();
        jdbc.update("INSERT INTO cv_intake_item (id, batch_id, uploaded_by, uploaded_at, expires_at) "
                + "VALUES (?, ?, ?, now(), now() + interval '14 days')", rawId, UUID.randomUUID(), uploader);
        assertThat(jdbc.queryForObject("SELECT detected->'emails' FROM cv_intake_item WHERE id = ?", String.class,
                rawId)).isEqualTo("[]");
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_cv_intake_detected'",
                String.class)).contains("USING gin (detected)");
    }

    @Test
    void intakeItemChecksAndCascadeOnAttachedPerson() {
        UUID uploader = newUser().getId();
        CvIntakeItem i = intakeItem(uploader, "a@example.test");
        assertThatThrownBy(() -> jdbc.update("UPDATE cv_intake_item SET status = 'DONE' WHERE id = ?", i.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE cv_intake_item SET size_bytes = 99999999 WHERE id = ?", i.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE cv_intake_item SET source_channel = 'FAX' WHERE id = ?", i.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID personId = person(uploader);
        jdbc.update("UPDATE cv_intake_item SET status = 'ATTACHED', attached_person_id = ? WHERE id = ?",
                personId, i.getId());
        assertThat(intake.findByAttachedPersonId(personId)).hasSize(1);
        tx.executeWithoutResult(s -> people.deleteById(personId));
        assertThat(intake.findById(i.getId())).isEmpty();
    }

    // ---------------------------------------------------------------- duplicate_candidate

    @Test
    void duplicatePairIsOrderedUniqueAndCascades() {
        UUID owner = newUser().getId();
        UUID one = person(owner);
        UUID two = person(owner);
        DuplicateCandidate d = tx.execute(s -> duplicates.saveAndFlush(DuplicateCandidate.of(
                one, two, List.of("EMAIL", "PHONE"))));
        assertThat(d.getPersonAId().compareTo(d.getPersonBId())).isNegative();
        assertThat(d.getStatus()).isEqualTo(DuplicateStatus.OPEN);
        assertThat(duplicates.findByPersonAIdAndPersonBId(d.getPersonAId(), d.getPersonBId())).isPresent();
        assertThat(duplicates.findByPerson(one)).hasSize(1);
        assertThat(duplicates.findByPerson(two)).hasSize(1);
        assertThat(duplicates.findByStatusOrderByDetectedAtDesc(DuplicateStatus.OPEN,
                org.springframework.data.domain.PageRequest.of(0, 1000)).getContent())
                .extracting(DuplicateCandidate::getId).contains(d.getId());

        // same pair again, in either order, is rejected
        assertThatThrownBy(() -> tx.execute(s -> duplicates.saveAndFlush(DuplicateCandidate.of(two, one, List.of("EMAIL")))))
                .isInstanceOf(DataIntegrityViolationException.class);
        // reversed order and a pair of one are rejected by the CHECK
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO duplicate_candidate (id, person_a_id, person_b_id, reasons, detected_at) "
                        + "VALUES (?, ?, ?, ARRAY['EMAIL'], now())", UUID.randomUUID(), d.getPersonBId(), d.getPersonAId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO duplicate_candidate (id, person_a_id, person_b_id, reasons, detected_at) "
                        + "VALUES (?, ?, ?, ARRAY['EMAIL'], now())", UUID.randomUUID(), one, one))
                .isInstanceOf(DataIntegrityViolationException.class);
        // reasons: at least one, and only contract values
        UUID three = person(owner);
        assertThatThrownBy(() -> tx.execute(s -> duplicates.saveAndFlush(DuplicateCandidate.of(one, three, List.of()))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> tx.execute(s -> duplicates.saveAndFlush(
                DuplicateCandidate.of(one, three, List.of("SHOE_SIZE")))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE duplicate_candidate SET status = 'WAITING' WHERE id = ?", d.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        // cascade from either end
        tx.executeWithoutResult(s -> people.deleteById(two));
        assertThat(duplicates.findById(d.getId())).isEmpty();
    }

    // ---------------------------------------------------------------- person_merge

    @Test
    void personMergeKeepsIdsOnlyIsUniquePerMergedIdAndCascadesFromSurvivor() {
        UUID owner = newUser().getId();
        UUID survivor = person(owner);
        UUID mergedId = UUID.randomUUID();
        PersonMerge m = new PersonMerge();
        m.setSurvivorId(survivor);
        m.setMergedId(mergedId);
        m.setFieldsTaken(List.of("email", "phone"));
        m.setMergedBy(owner);
        tx.execute(s -> merges.saveAndFlush(m));

        assertThat(merges.findMergedIds(survivor)).containsExactly(mergedId);
        assertThat(merges.findBySurvivorId(survivor)).singleElement()
                .satisfies(x -> assertThat(x.getFieldsTaken()).containsExactly("email", "phone"));
        // a merged person can only be merged once
        PersonMerge again = new PersonMerge();
        again.setSurvivorId(person(owner));
        again.setMergedId(mergedId);
        again.setFieldsTaken(List.of());
        again.setMergedBy(owner);
        assertThatThrownBy(() -> tx.execute(s -> merges.saveAndFlush(again)))
                .isInstanceOf(DataIntegrityViolationException.class);

        tx.executeWithoutResult(s -> people.deleteById(survivor));
        assertThat(merges.findMergedIds(survivor)).isEmpty();
    }

    // ---------------------------------------------------------------- off_limits_rule

    private OffLimitsRule rule(UUID accountId, String company, LocalDate start, LocalDate end) {
        OffLimitsRule r = new OffLimitsRule();
        r.setAccountId(accountId);
        r.setCompanyName(company);
        r.setReason(OffLimitsReason.CLIENT_AGREEMENT);
        r.setStartsOn(start);
        r.setEndsOn(end);
        return tx.execute(s -> offLimits.saveAndFlush(r));
    }

    @Test
    void offLimitsRuleTargetDatesAndOpenRuleUniqueness() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        LocalDate today = LocalDate.now();
        OffLimitsRule byAccount = rule(account, null, today.minusDays(5), null);
        assertThat(byAccount.getVersion()).isNotNull();
        assertThat(count("SELECT count(*) FROM off_limits_rule_aud WHERE id = ?", byAccount.getId())).isOne();

        // exactly one target
        assertThatThrownBy(() -> rule(null, null, today, null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> rule(account, "Both Ltd", today, null)).isInstanceOf(DataIntegrityViolationException.class);
        // end before start
        assertThatThrownBy(() -> rule(account, null, today, today.minusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
        // second open rule for the same account
        assertThatThrownBy(() -> rule(account, null, today, null)).isInstanceOf(DataIntegrityViolationException.class);
        // closed rules may repeat, and a new open one follows once the first is closed
        rule(account, null, today.minusDays(60), today.minusDays(30));
        rule(account, null, today.minusDays(29), today.minusDays(10));
        byAccount.setEndsOn(today.plusDays(1));
        tx.execute(s -> offLimits.saveAndFlush(byAccount));
        rule(account, null, today.plusDays(2), null);

        // company rules compare accent- and case-insensitively
        String name = unique("Çelik Holding");
        rule(null, name, today, null);
        assertThatThrownBy(() -> rule(null, "  " + name.toUpperCase().replace("Ç", "c") + " ", today, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE off_limits_rule SET reason = 'BECAUSE' WHERE id = ?",
                byAccount.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void offLimitsActiveLookup() {
        UUID owner = newUser().getId();
        UUID account = account(owner);
        String company = unique("Kırmızı Ltd");
        LocalDate today = LocalDate.now();
        OffLimitsRule open = rule(account, null, today.minusDays(1), null);
        OffLimitsRule ended = rule(null, company, today.minusDays(20), today.minusDays(2));
        OffLimitsRule byName = rule(null, company + " 2", today, today.plusDays(10));

        assertThat(offLimits.findActive(account, "nobody", today)).extracting(OffLimitsRule::getId)
                .containsExactly(open.getId());
        assertThat(offLimits.findActive(UUID.randomUUID(), company, today)).isEmpty();
        assertThat(offLimits.findActive(UUID.randomUUID(), company, today.minusDays(5))).extracting(OffLimitsRule::getId)
                .containsExactly(ended.getId());
        assertThat(offLimits.findActive(UUID.randomUUID(), (company + " 2").toUpperCase().replace("I", "ı"), today)
                .stream().map(OffLimitsRule::getId)).containsExactly(byName.getId());
    }
}
