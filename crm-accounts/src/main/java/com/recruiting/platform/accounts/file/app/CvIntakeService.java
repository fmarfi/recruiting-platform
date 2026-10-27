package com.recruiting.platform.accounts.file.app;

import com.recruiting.platform.accounts.CvIntakeApi;
import com.recruiting.platform.accounts.domain.CvIntakeItem;
import com.recruiting.platform.accounts.domain.CvIntakeItemRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.duplicate.app.DuplicateService;
import com.recruiting.platform.accounts.duplicate.web.dto.DuplicateMatchResponse;
import com.recruiting.platform.accounts.file.web.dto.CvIntakeDtos;
import com.recruiting.platform.accounts.person.app.PersonMapper;
import com.recruiting.platform.accounts.person.app.PersonService;
import com.recruiting.platform.accounts.person.web.dto.PersonCreateRequest;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.AfterCommit;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.accounts.shared.dto.PersonRef;
import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.CvIntakeStatus;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.contracts.SourceChannel;
import com.recruiting.platform.contracts.TextExtractionStatus;
import com.recruiting.platform.security.AccessRecorder;
import com.recruiting.platform.security.UserDirectory;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** CV intake queue (B-01, ADR 0011): bulk upload, review, attach or discard, expiry after 14 days. */
@Service
@Transactional
public class CvIntakeService implements CvIntakeApi {

    private static final Logger LOG = LoggerFactory.getLogger(CvIntakeService.class);
    private static final Duration EXPIRY = Duration.ofDays(14);

    private final CvIntakeItemRepository items;
    private final PersonRepository people;
    private final FileStore store;
    private final FileTypes types;
    private final PersonFileService fileService;
    private final PersonService personService;
    private final PersonMapper mapper;
    private final DuplicateService duplicates;
    private final UserDirectory users;
    private final AccessRecorder access;

    public CvIntakeService(CvIntakeItemRepository items, PersonRepository people, FileStore store, FileTypes types,
            PersonFileService fileService, PersonService personService, PersonMapper mapper,
            DuplicateService duplicates, UserDirectory users, AccessRecorder access) {
        this.items = items;
        this.people = people;
        this.store = store;
        this.types = types;
        this.fileService = fileService;
        this.personService = personService;
        this.mapper = mapper;
        this.duplicates = duplicates;
        this.users = users;
        this.access = access;
    }

    public CvIntakeDtos.Batch upload(List<MultipartFile> uploads, SourceChannel channel, Actor actor) {
        List<MultipartFile> given = uploads == null ? List.of()
                : uploads.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (given.isEmpty() || given.size() > 50) {
            throw PatchBinder.validation("files", "SIZE", "send between 1 and 50 files");
        }
        UUID batchId = UUID.randomUUID();
        Instant now = Instant.now();
        List<CvIntakeDtos.Item> accepted = new ArrayList<>();
        List<CvIntakeDtos.Rejected> rejected = new ArrayList<>();
        for (MultipartFile f : given) {
            String name = FileTypes.cleanName(f.getOriginalFilename());
            if (f.getSize() > FileTypes.MAX_BYTES) {
                rejected.add(new CvIntakeDtos.Rejected(name, ErrorCode.PAYLOAD_TOO_LARGE.name()));
                continue;
            }
            byte[] bytes;
            try {
                bytes = f.getBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            String type = types.detect(bytes);
            if (!types.allowed(type)) {
                rejected.add(new CvIntakeDtos.Rejected(name, ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()));
                continue;
            }
            CvIntakeItem item = new CvIntakeItem();
            item.setBatchId(batchId);
            item.setFileName(name);
            item.setContentType(type);
            item.setSizeBytes((long) bytes.length);
            item.setSha256(FileTypes.sha256(bytes));
            item.setStorageKey("tmp/" + UUID.randomUUID());
            item.setTextStatus(TextExtractionStatus.PENDING);
            item.setSourceChannel(channel == null ? SourceChannel.CV_UPLOAD : channel);
            item.setUploadedBy(actor.id());
            item.setUploadedAt(now);
            item.setExpiresAt(now.plus(EXPIRY));
            item = items.saveAndFlush(item);
            item.setStorageKey("intake/" + item.getId());
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                store.put(item.getStorageKey(), in, bytes.length, type);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            accepted.add(response(items.save(item)));
        }
        return new CvIntakeDtos.Batch(batchId, accepted, rejected);
    }

    @Transactional(readOnly = true)
    public PageResponse<CvIntakeDtos.Item> list(List<CvIntakeStatus> status, UUID uploadedBy, UUID batchId,
            Integer page, Integer size, List<String> sort) {
        List<CvIntakeStatus> wanted = status == null || status.isEmpty() ? List.of(CvIntakeStatus.PENDING) : status;
        Specification<CvIntakeItem> spec = (root, q, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(root.get("status").in(wanted));
            if (uploadedBy != null) {
                all.add(cb.equal(root.get("uploadedBy"), uploadedBy));
            }
            if (batchId != null) {
                all.add(cb.equal(root.get("batchId"), batchId));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
        var pageable = Paging.of(page, size, sort, Map.of("uploadedAt", "uploadedAt"),
                Sort.by(Sort.Order.desc("uploadedAt")));
        Page<CvIntakeItem> result = items.findAll(spec, pageable);
        return PageResponse.of(result, result.getContent().stream().map(this::response).toList());
    }

    @Transactional(readOnly = true)
    public CvIntakeDtos.Item get(UUID id) {
        return response(require(id));
    }

    public void discard(UUID id) {
        CvIntakeItem item = require(id);
        if (item.getStatus() != CvIntakeStatus.PENDING) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Only pending items can be discarded.");
        }
        minimise(item, CvIntakeStatus.DISCARDED, null);
    }

    public PersonFileService.Download download(UUID id) {
        CvIntakeItem item = require(id);
        if (item.getStatus() != CvIntakeStatus.PENDING || item.getStorageKey() == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "The file of item " + id + " is no longer available.");
        }
        access.record(AccessAction.DOWNLOAD, EntityType.CV_INTAKE_ITEM, item.getId());
        return new PersonFileService.Download(item.getFileName(), readBytes(item));
    }

    public CvIntakeDtos.AttachResult attach(UUID id, CvIntakeDtos.AttachRequest request, Actor actor) {
        CvIntakeItem item = require(id);
        if (item.getStatus() != CvIntakeStatus.PENDING || item.getStorageKey() == null) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Only pending items can be attached.");
        }
        if ((request.personId() == null) == (request.person() == null)) {
            throw PatchBinder.validation("body", "ONE_OF", "send exactly one of personId and person");
        }
        byte[] bytes = readBytes(item);
        Person person;
        if (request.personId() != null) {
            person = people.findById(request.personId()).orElseThrow(
                    () -> new ApiException(ErrorCode.NOT_FOUND, "Person " + request.personId() + " not found."));
            if (person.getAnonymisedAt() != null) {
                throw new ApiException(ErrorCode.STATE_CONFLICT, "Anonymised people cannot receive files.");
            }
        } else {
            PersonCreateRequest r = request.person();
            if (r.sourceChannel() == null) {
                r = new PersonCreateRequest(r.name(), r.type(), r.title(), r.company(), r.email(), r.phone(),
                        r.city(), r.linkedinUrl(), r.howWeKnow(), r.notes(), r.ownerId(), r.employments(),
                        item.getSourceChannel(), r.noticePeriodDays(), r.mobility(), r.compensation(), r.languages(),
                        r.educations());
            }
            person = personService.createEntity(r, actor, null);
        }
        PersonFile file = fileService.add(person, bytes, item.getFileName(), item.getContentType(),
                PersonFileKind.CV, actor, item.getTextStatus(), item.getExtractedText());
        minimise(item, CvIntakeStatus.ATTACHED, person.getId());
        duplicates.record(person);
        return new CvIntakeDtos.AttachResult(personService.get(person.getId()), mapper.file(file));
    }

    // ---------------------------------------------------------------- CvIntakeApi (DSRs)

    @Override
    @Transactional(readOnly = true)
    public List<PendingIntake> findPendingByEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return items.findPendingByDetectedEmail(email.trim().toLowerCase(Locale.ROOT)).stream()
                .map(i -> new PendingIntake(i.getId(), i.getFileName(), i.getUploadedAt(),
                        List.copyOf(i.getDetected().getOrDefault("emails", List.of())),
                        List.copyOf(i.getDetected().getOrDefault("phones", List.of())),
                        List.copyOf(i.getDetected().getOrDefault("linkedinUrls", List.of()))))
                .toList();
    }

    @Override
    public int discardPendingByEmail(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        List<CvIntakeItem> found = items.findPendingByDetectedEmail(email.trim().toLowerCase(Locale.ROOT));
        found.forEach(i -> minimise(i, CvIntakeStatus.DISCARDED, null));
        return found.size();
    }

    // ---------------------------------------------------------------- nightly expiry

    /** Pending items expire 14 days after upload; finished items are removed after 30 days. */
    @Scheduled(cron = "${app.files.intake-expiry-cron:0 15 3 * * *}")
    public void expire() {
        int n = 0;
        for (CvIntakeItem i : items.findByStatusAndExpiresAtBefore(CvIntakeStatus.PENDING, Instant.now())) {
            deleteBytes(i.getStorageKey());
            items.delete(i);
            n++;
        }
        for (CvIntakeItem i : items.findByStatusInAndUploadedAtBefore(
                EnumSet.of(CvIntakeStatus.ATTACHED, CvIntakeStatus.DISCARDED), Instant.now().minus(Duration.ofDays(30)))) {
            items.delete(i);
            n++;
        }
        if (n > 0) {
            LOG.info("CV intake expiry removed {} items", n);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void minimise(CvIntakeItem item, CvIntakeStatus status, UUID personId) {
        String key = item.getStorageKey();
        item.setStatus(status);
        item.setAttachedPersonId(personId);
        item.setFileName(null);
        item.setContentType(null);
        item.setSizeBytes(null);
        item.setSha256(null);
        item.setStorageKey(null);
        item.setExtractedText(null);
        item.setDetected(null);
        items.saveAndFlush(item);
        deleteBytes(key);
    }

    private void deleteBytes(String key) {
        if (key == null) {
            return;
        }
        AfterCommit.run(() -> {
            try {
                store.delete(key);
            } catch (IOException e) {
                LOG.warn("Could not delete stored bytes {}; the weekly sweep removes them", key);
            }
        });
    }

    private byte[] readBytes(CvIntakeItem item) {
        try {
            InputStream in = store.get(item.getStorageKey())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "The file content is missing."));
            try (in) {
                return in.readAllBytes();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CvIntakeItem require(UUID id) {
        return items.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Intake item " + id + " not found."));
    }

    private CvIntakeDtos.Item response(CvIntakeItem i) {
        Map<String, List<String>> d = i.getDetected();
        List<String> emails = d.getOrDefault("emails", List.of());
        List<String> phones = d.getOrDefault("phones", List.of());
        List<String> linkedin = d.getOrDefault("linkedinUrls", List.of());
        List<DuplicateMatchResponse> matches = i.getStatus() == CvIntakeStatus.PENDING
                ? matches(emails, phones, linkedin) : List.of();
        PersonRef attached = null;
        if (i.getAttachedPersonId() != null) {
            attached = people.findById(i.getAttachedPersonId()).map(p -> new PersonRef(p.getId(), p.getName()))
                    .orElse(null);
        }
        return new CvIntakeDtos.Item(i.getId(), i.getBatchId(), i.getStatus(), i.getFileName(), i.getContentType(),
                i.getSizeBytes(), i.getTextStatus(), new CvIntakeDtos.Detected(emails, phones, linkedin), matches,
                i.getSourceChannel(), i.getUploadedAt(), users.ref(i.getUploadedBy()).orElse(null), i.getExpiresAt(),
                attached);
    }

    private List<DuplicateMatchResponse> matches(List<String> emails, List<String> phones, List<String> linkedin) {
        Map<UUID, DuplicateService.Match> merged = new LinkedHashMap<>();
        List<DuplicateService.Match> all = new ArrayList<>();
        emails.forEach(e -> all.addAll(duplicates.find(null, null, e, null, null, null)));
        phones.forEach(p -> all.addAll(duplicates.find(null, null, null, p, null, null)));
        linkedin.forEach(l -> all.addAll(duplicates.find(null, null, null, null, l, null)));
        for (DuplicateService.Match m : all) {
            merged.merge(m.person().getId(), m, (a, b) -> {
                var reasons = new java.util.TreeSet<>(a.reasons());
                reasons.addAll(b.reasons());
                return new DuplicateService.Match(a.person(), List.copyOf(reasons));
            });
        }
        List<DuplicateService.Match> sorted = merged.values().stream()
                .sorted(java.util.Comparator.comparing(m -> m.reasons().get(0))).limit(10).toList();
        List<com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse> summaries = mapper
                .summaries(sorted.stream().map(DuplicateService.Match::person).toList());
        List<DuplicateMatchResponse> out = new ArrayList<>();
        for (int k = 0; k < sorted.size(); k++) {
            out.add(new DuplicateMatchResponse(summaries.get(k), sorted.get(k).reasons()));
        }
        return out;
    }
}
