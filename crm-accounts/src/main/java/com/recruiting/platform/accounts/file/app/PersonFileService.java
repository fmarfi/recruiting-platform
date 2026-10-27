package com.recruiting.platform.accounts.file.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.web.dto.PersonFileResponse;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.AfterCommit;
import com.recruiting.platform.accounts.shared.Events;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.PersonFileUploaded;
import com.recruiting.platform.contracts.TextExtractionStatus;
import com.recruiting.platform.security.AccessRecorder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** CVs and documents of a person (B-01, ADR 0011): upload with type and size checks, list, download, delete. */
@Service
@Transactional
public class PersonFileService {

    private static final Logger LOG = LoggerFactory.getLogger(PersonFileService.class);

    private final PersonRepository people;
    private final PersonFileRepository files;
    private final FileStore store;
    private final FileTypes types;
    private final AccessRecorder access;
    private final PersonMapper mapper;
    private final ApplicationEventPublisher events;

    public PersonFileService(PersonRepository people, PersonFileRepository files, FileStore store, FileTypes types,
            AccessRecorder access, PersonMapper mapper, ApplicationEventPublisher events) {
        this.people = people;
        this.files = files;
        this.store = store;
        this.types = types;
        this.access = access;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<PersonFileResponse> list(UUID personId) {
        requirePerson(personId);
        return files.findByPersonIdOrderByKindAscVersionDesc(personId).stream()
                .sorted(Comparator.comparing(PersonFile::getCreatedAt).reversed()).map(mapper::file).toList();
    }

    public PersonFileResponse upload(UUID personId, MultipartFile upload, PersonFileKind kind, Actor actor) {
        Person person = requirePerson(personId);
        if (person.getAnonymisedAt() != null) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Anonymised people cannot receive files.");
        }
        if (upload == null || upload.isEmpty()) {
            throw PatchBinder.validation("file", "REQUIRED", "a non-empty file is required");
        }
        if (upload.getSize() > FileTypes.MAX_BYTES) {
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, "Files may be at most 10 MB.");
        }
        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String contentType = types.detect(bytes);
        if (!types.allowed(contentType)) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, DOCX, DOC, RTF, ODT and TXT files are accepted.");
        }
        PersonFile file = add(person, bytes, FileTypes.cleanName(upload.getOriginalFilename()), contentType,
                kind == null ? PersonFileKind.CV : kind, actor, TextExtractionStatus.PENDING, null);
        return mapper.file(file);
    }

    /** Stores bytes and row; also used by the CV intake attach. Text may already be known (intake). */
    public PersonFile add(Person person, byte[] bytes, String fileName, String contentType, PersonFileKind kind,
            Actor actor, TextExtractionStatus textStatus, String text) {
        String sha = FileTypes.sha256(bytes);
        files.findByPersonIdAndSha256(person.getId(), sha).ifPresent(existing -> {
            throw new ApiException(ErrorCode.DUPLICATE, "This file is already stored for the person.",
                    Map.of("existingId", existing.getId()));
        });
        int version = 1;
        if (kind == PersonFileKind.CV) {
            files.findByPersonIdAndKindAndCurrentTrue(person.getId(), PersonFileKind.CV).ifPresent(prev -> {
                prev.setCurrent(false);
                files.saveAndFlush(prev);
            });
            version = files.maxVersion(person.getId(), PersonFileKind.CV) + 1;
        }
        PersonFile f = new PersonFile();
        f.setPersonId(person.getId());
        f.setKind(kind);
        f.setVersion(version);
        f.setCurrent(kind == PersonFileKind.CV);
        f.setFileName(fileName);
        f.setContentType(contentType);
        f.setSizeBytes((long) bytes.length);
        f.setSha256(sha);
        f.setStorageKey("tmp/" + UUID.randomUUID());
        f.setTextStatus(textStatus);
        f.setExtractedText(text);
        f.setUploadedBy(actor.id());
        f = files.saveAndFlush(f);
        f.setStorageKey("person/" + person.getId() + "/" + f.getId());
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            store.put(f.getStorageKey(), in, bytes.length, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        f = files.saveAndFlush(f);
        events.publishEvent(new PersonFileUploaded(Events.newId(), Instant.now(), actor.id(), person.getId(),
                f.getId(), f.getKind(), f.getVersion()));
        return f;
    }

    /** Download bytes plus the file name; writes the DOWNLOAD access-log row. */
    public Download download(UUID personId, UUID fileId) {
        PersonFile f = require(personId, fileId);
        access.record(AccessAction.DOWNLOAD, EntityType.PERSON_FILE, f.getId());
        try {
            InputStream in = store.get(f.getStorageKey())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "The file content is missing."));
            try (in) {
                return new Download(f.getFileName(), in.readAllBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete(UUID personId, UUID fileId) {
        PersonFile f = require(personId, fileId);
        String key = f.getStorageKey();
        boolean wasCurrentCv = f.isCurrent();
        files.delete(f);
        files.flush();
        if (wasCurrentCv) {
            files.findByPersonIdOrderByKindAscVersionDesc(personId).stream()
                    .filter(o -> o.getKind() == PersonFileKind.CV).findFirst().ifPresent(next -> {
                        next.setCurrent(true);
                        files.save(next);
                    });
        }
        AfterCommit.run(() -> deleteBytes(key));
    }

    private void deleteBytes(String key) {
        try {
            store.delete(key);
        } catch (IOException e) {
            LOG.warn("Could not delete stored bytes {}; the weekly sweep removes them", key);
        }
    }

    private Person requirePerson(UUID personId) {
        return people.findById(personId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Person " + personId + " not found."));
    }

    private PersonFile require(UUID personId, UUID fileId) {
        return files.findById(fileId).filter(f -> f.getPersonId().equals(personId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "File " + fileId + " not found."));
    }

    public record Download(String fileName, byte[] bytes) {
    }
}
