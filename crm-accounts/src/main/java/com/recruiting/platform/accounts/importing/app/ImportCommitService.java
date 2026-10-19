package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.accounts.account.app.AccountService;
import com.recruiting.platform.accounts.contact.app.ContactService;
import com.recruiting.platform.accounts.contact.web.dto.ContactCreateRequest;
import com.recruiting.platform.accounts.domain.ImportJob;
import com.recruiting.platform.accounts.domain.ImportJobRepository;
import com.recruiting.platform.accounts.importing.app.ImportAnalysis.Row;
import com.recruiting.platform.accounts.importing.web.dto.ImportJobResponse;
import com.recruiting.platform.accounts.person.app.PersonService;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ImportCompleted;
import com.recruiting.platform.contracts.ImportStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Commits a validated import (A-11): re-validates the stored file (the data may have changed since the upload),
 * writes the valid rows in one transaction and publishes ImportCompleted. If anything fails, nothing is written and
 * the job becomes FAILED. The stored file and error list are dropped when the job ends (ADR 0009).
 */
@Service
public class ImportCommitService {

    private final ImportJobRepository jobs;
    private final ImportAnalyzer analyzer;
    private final AccountService accountService;
    private final PersonService personService;
    private final ContactService contactService;
    private final ImportJobMapper mapper;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final TransactionTemplate txNew;

    public ImportCommitService(ImportJobRepository jobs, ImportAnalyzer analyzer, AccountService accountService,
            PersonService personService, ContactService contactService, ImportJobMapper mapper,
            ApplicationEventPublisher events, PlatformTransactionManager tm) {
        this.jobs = jobs;
        this.analyzer = analyzer;
        this.accountService = accountService;
        this.personService = personService;
        this.contactService = contactService;
        this.mapper = mapper;
        this.events = events;
        this.tx = new TransactionTemplate(tm);
        this.txNew = new TransactionTemplate(tm);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ImportJobResponse commit(UUID id, Actor actor) {
        try {
            return tx.execute(status -> write(id, actor));
        } catch (ApiException e) {
            if (e.code() == ErrorCode.NOT_FOUND || e.code() == ErrorCode.STATE_CONFLICT) {
                throw e;
            }
            return fail(id, e.getMessage());
        } catch (RuntimeException e) {
            return fail(id, "The import could not be written: " + e.getClass().getSimpleName());
        }
    }

    private ImportJobResponse write(UUID id, Actor actor) {
        ImportJob job = jobs.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Import " + id + " not found."));
        if (job.getStatus() != ImportStatus.VALIDATED) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Only a validated import can be committed.",
                    Map.of("currentStatus", job.getStatus().name()));
        }
        ImportAnalysis analysis = analyzer.analyze(job.getKind(), job.getFileName(), job.getFileContent());
        for (Row row : analysis.valid()) {
            writeRow(row, actor, job.getId());
        }
        job.setStatus(ImportStatus.COMPLETED);
        job.setImportedRows(analysis.valid().size());
        job.setValidRows(analysis.valid().size());
        job.setDuplicateRows(analysis.duplicateRows());
        job.setFileContent(null);
        job.setErrors(null);
        job.setCompletedAt(Instant.now());
        job = jobs.saveAndFlush(job);
        events.publishEvent(new ImportCompleted(Events.newId(), Instant.now(), actor.id(), job.getId(), job.getKind(),
                analysis.valid().size()));
        return mapper.response(job);
    }

    private void writeRow(Row row, Actor actor, UUID importId) {
        if (row.account() != null) {
            accountService.create(row.account(), actor);
            return;
        }
        UUID personId = personService.createEntity(row.person(), actor, importId).getId();
        if (row.accountId() != null) {
            contactService.add(row.accountId(), new ContactCreateRequest(personId, null, row.role(), false), actor);
        }
    }

    private ImportJobResponse fail(UUID id, String reason) {
        return txNew.execute(status -> {
            ImportJob job = jobs.findById(id).orElseThrow();
            job.setStatus(ImportStatus.FAILED);
            job.setFailureReason(reason);
            job.setFileContent(null);
            job.setErrors(null);
            job.setCompletedAt(Instant.now());
            return mapper.response(jobs.saveAndFlush(job));
        });
    }
}
