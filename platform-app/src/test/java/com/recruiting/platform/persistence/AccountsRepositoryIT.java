package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountOpenJob;
import com.recruiting.platform.accounts.domain.AccountOpenJobRepository;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Contact;
import com.recruiting.platform.accounts.domain.ContactRepository;
import com.recruiting.platform.accounts.domain.ImportJob;
import com.recruiting.platform.accounts.domain.ImportJobRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.ImportStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AccountsRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PersonRepository people;

    @Autowired
    private ContactRepository contacts;

    @Autowired
    private AccountOpenJobRepository openJobs;

    @Autowired
    private ImportJobRepository imports;

    @Autowired
    private EntityManager em;

    private Account newAccount(UUID owner, String name) {
        Account a = new Account();
        a.setName(name);
        a.setSector("Energy");
        a.setOwnerId(owner);
        a.setStatus(AccountStatus.LEAD);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        return tx.execute(s -> accounts.save(a));
    }

    private Person newPerson(UUID owner, String name, String email) {
        Person p = new Person();
        p.setName(name);
        p.setType(PersonType.CANDIDATE);
        p.setEmail(email);
        p.setOwnerId(owner);
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> people.save(p));
    }

    @Test
    void minimallyPopulatedEntitiesPersistWithDatabaseDefaults() {
        UUID owner = newUser().getId();
        Account a = new Account();
        a.setName("Minimal Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(owner);
        Person p = new Person();
        p.setName("Minimal Person " + UUID.randomUUID());
        p.setType(PersonType.REFERRER);
        p.setOwnerId(owner);
        tx.executeWithoutResult(s -> {
            accounts.save(a);
            people.save(p);
        });
        Account loadedA = accounts.findById(a.getId()).orElseThrow();
        assertThat(loadedA.getStatus()).isEqualTo(AccountStatus.LEAD);
        assertThat(loadedA.getOpenJobCount()).isZero();
        Person loadedP = people.findById(p.getId()).orElseThrow();
        assertThat(loadedP.getConsentStatus()).isEqualTo(ConsentStatus.NOT_SENT);
        assertThat(loadedP.getRetentionStatus()).isEqualTo(RetentionStatus.ACTIVE);
    }

    @Test
    void purgeFilesClearsOldUploads() {
        ImportJob job = new ImportJob();
        job.setKind(ImportKind.ACCOUNTS);
        job.setStatus(ImportStatus.VALIDATED);
        job.setFileContent(new byte[] {9});
        UUID id = tx.execute(s -> imports.save(job)).getId();
        int changed = tx.execute(s -> imports.purgeFilesCreatedBefore(Instant.now().plusSeconds(60)));
        assertThat(changed).isGreaterThanOrEqualTo(1);
        byte[] content = tx.execute(s -> imports.findById(id).orElseThrow().getFileContent());
        assertThat(content).isNull();
    }

    @Test
    void trigramSearchIgnoresCaseAndAccents() {
        UUID owner = newUser().getId();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        newPerson(owner, "Ayşe Öztürk " + tag, "ayse." + tag + "@example.test");
        newPerson(owner, "Someone Else " + tag, null);

        List<Person> byAscii = people.searchText("OZTURK " + tag, 10);
        assertThat(byAscii).hasSize(1);
        assertThat(byAscii.get(0).getName()).contains("Öztürk");
        assertThat(people.searchText("ayse." + tag.toUpperCase(), 10)).hasSize(1);

        newAccount(owner, "Şeker İş Holding " + tag);
        assertThat(accounts.searchByName("seker is holding " + tag, 10)).hasSize(1);
    }

    @Test
    void uniqueNameEmailAndActiveContactRulesHold() {
        UUID owner = newUser().getId();
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Account a = newAccount(owner, "Unique Corp " + tag);
        assertThatThrownBy(() -> newAccount(owner, "UNIQUE CORP " + tag)).isInstanceOf(DataIntegrityViolationException.class);

        Person p = newPerson(owner, "Contact Person " + tag, "c." + tag + "@example.test");
        assertThatThrownBy(() -> newPerson(owner, "Other " + tag, "C." + tag + "@EXAMPLE.test"))
                .isInstanceOf(DataIntegrityViolationException.class);

        Contact c = new Contact();
        c.setAccountId(a.getId());
        c.setPersonId(p.getId());
        c.setPrimary(true);
        tx.executeWithoutResult(s -> contacts.save(c));
        assertThat(contacts.findByAccountIdAndEndedOnIsNull(a.getId())).hasSize(1);
        Contact dup = new Contact();
        dup.setAccountId(a.getId());
        dup.setPersonId(p.getId());
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> contacts.save(dup)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void openJobProjectionUpsertsByJobOrderId() {
        UUID owner = newUser().getId();
        Account a = newAccount(owner, "Projection Corp " + UUID.randomUUID());
        UUID jobId = UUID.randomUUID();
        AccountOpenJob j = new AccountOpenJob(jobId);
        j.setAccountId(a.getId());
        j.setTitle("CFO");
        j.setOpen(true);
        j.setUpdatedAt(Instant.now());
        tx.executeWithoutResult(s -> openJobs.save(j));
        assertThat(openJobs.countByAccountIdAndOpenTrue(a.getId())).isEqualTo(1);

        tx.executeWithoutResult(s -> {
            AccountOpenJob loaded = openJobs.findById(jobId).orElseThrow();
            loaded.setOpen(false);
            openJobs.save(loaded);
        });
        assertThat(openJobs.countByAccountIdAndOpenTrue(a.getId())).isZero();
    }

    @Test
    void importJobKeepsFileAndJsonErrors() {
        ImportJob job = new ImportJob();
        job.setKind(ImportKind.PEOPLE);
        job.setStatus(ImportStatus.VALIDATED);
        job.setFileName("people.csv");
        job.setFileContent(new byte[] {1, 2, 3});
        job.setErrors(List.of(Map.of("row", 2, "column", "email", "message", "invalid")));
        ImportJob saved = tx.execute(s -> imports.save(job));
        ImportJob loaded = tx.execute(s -> {
            ImportJob j = imports.findById(saved.getId()).orElseThrow();
            assertThat(j.getFileContent()).containsExactly(1, 2, 3);
            return j;
        });
        assertThat(loaded.getErrors()).hasSize(1);
        assertThat(loaded.getErrors().get(0)).containsEntry("column", "email");
    }

    @Test
    void enversRecordsOneRevisionPerChange() {
        UUID owner = newUser().getId();
        Account a = newAccount(owner, "Audited Corp " + UUID.randomUUID());

        tx.executeWithoutResult(s -> {
            Account x = accounts.findById(a.getId()).orElseThrow();
            x.setStatus(AccountStatus.PROPOSAL_SENT);
            x.setStatusChangedAt(Instant.now());
        });

        tx.executeWithoutResult(s -> {
            var reader = AuditReaderFactory.get(em);
            List<Number> revisions = reader.getRevisions(Account.class, a.getId());
            assertThat(revisions).hasSize(2);
            Account first = reader.find(Account.class, a.getId(), revisions.get(0));
            Account second = reader.find(Account.class, a.getId(), revisions.get(1));
            assertThat(first.getStatus()).isEqualTo(AccountStatus.LEAD);
            assertThat(second.getStatus()).isEqualTo(AccountStatus.PROPOSAL_SENT);
        });
    }
}
