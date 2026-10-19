package com.recruiting.platform.privacy.dsr.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.DataSubjectRequestStatus;
import com.recruiting.platform.contracts.DataSubjectRequestType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.IllegalTransitionException;
import com.recruiting.platform.contracts.RetentionAction;
import com.recruiting.platform.privacy.domain.DataSubjectRequest;
import com.recruiting.platform.privacy.domain.DataSubjectRequestRepository;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrCreate;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrResponse;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.DsrUpdate;
import com.recruiting.platform.privacy.dsr.web.dto.DsrDtos.PersonRef;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Data-subject requests (S-04): logged with a 30-day deadline, handled along RECEIVED, IN_PROGRESS, COMPLETED or
 * REJECTED. Completing a DELETION request anonymises the linked person first; only when that works does the
 * request become COMPLETED.
 */
@Service
public class DataSubjectRequestService {

    private static final UserRef UNKNOWN = new UserRef(new UUID(0, 0), "System");
    private static final Set<DataSubjectRequestStatus> FINAL = Set.of(DataSubjectRequestStatus.COMPLETED,
            DataSubjectRequestStatus.REJECTED);

    private final DataSubjectRequestRepository requests;
    private final PersonGateway people;
    private final PersonErasureService erasure;
    private final UserDirectory directory;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final TransactionTemplate tx;

    DataSubjectRequestService(DataSubjectRequestRepository requests, PersonGateway people,
            PersonErasureService erasure, UserDirectory directory, CurrentUser currentUser, Clock clock,
            PlatformTransactionManager txManager) {
        this.requests = requests;
        this.people = people;
        this.erasure = erasure;
        this.directory = directory;
        this.currentUser = currentUser;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
    }

    public DsrResponse create(DsrCreate body) {
        if (body.personId() != null && people.find(body.personId()).isEmpty()) {
            throw notFound("Person " + body.personId());
        }
        return tx.execute(status -> {
            DataSubjectRequest request = new DataSubjectRequest();
            request.setPersonId(body.personId());
            request.setRequesterName(body.requesterName().trim());
            request.setRequesterEmail(body.requesterEmail().trim());
            request.setType(body.type());
            request.setDescription(body.description());
            if (body.receivedAt() != null) {
                request.setReceivedAt(body.receivedAt());
            } else {
                request.setReceivedAt(clock.instant());
            }
            request.setDueOn(request.getReceivedAt().atZone(clock.getZone()).toLocalDate().plusDays(30));
            return respond(List.of(requests.saveAndFlush(request))).get(0);
        });
    }

    public DsrResponse get(UUID id) {
        return tx.execute(status -> respond(List.of(load(id))).get(0));
    }

    public Page<DsrResponse> list(List<DataSubjectRequestStatus> statuses, DataSubjectRequestType type,
            UUID personId, Boolean overdue, Pageable pageable) {
        return tx.execute(status -> {
            Page<DataSubjectRequest> page = requests.findAll(filter(statuses, type, personId, overdue), pageable);
            List<DsrResponse> mapped = respond(page.getContent());
            return new org.springframework.data.domain.PageImpl<>(mapped, pageable, page.getTotalElements());
        });
    }

    public DsrResponse update(UUID id, DsrUpdate body) {
        DataSubjectRequest current = tx.execute(status -> {
            DataSubjectRequest request = load(id);
            check(request, body);
            return request;
        });
        boolean erase = body.status() == DataSubjectRequestStatus.COMPLETED
                && current.getType() == DataSubjectRequestType.DELETION;
        UUID personId = body.personId() != null ? body.personId() : current.getPersonId();
        if (erase) {
            if (personId == null) {
                throw new ApiException(ErrorCode.STATE_CONFLICT, "Link the person before completing a deletion request.");
            }
            erasure.erase(personId, RetentionAction.ANONYMISE, currentUser.id());
        }
        return tx.execute(status -> respond(List.of(apply(load(id), body))).get(0));
    }

    private void check(DataSubjectRequest request, DsrUpdate body) {
        if (body.version() != null && !body.version().equals(request.getVersion())) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT, "The request was changed by someone else.");
        }
        if (body.personId() != null && people.find(body.personId()).isEmpty()) {
            throw notFound("Person " + body.personId());
        }
        if (body.status() != null) {
            IllegalTransitionException.check(request.getStatus(), body.status(), body.reason());
        }
    }

    private DataSubjectRequest apply(DataSubjectRequest request, DsrUpdate body) {
        if (body.personId() != null) {
            request.setPersonId(body.personId());
        }
        if (body.handledById() != null) {
            request.setHandledBy(body.handledById());
        }
        if (body.resolution() != null) {
            request.setResolution(body.resolution());
        }
        if (body.status() != null) {
            request.setStatus(body.status());
            if (FINAL.contains(body.status())) {
                request.setResolvedAt(clock.instant());
                if (request.getHandledBy() == null) {
                    request.setHandledBy(currentUser.id());
                }
            }
            if (body.status() == DataSubjectRequestStatus.REJECTED && request.getResolution() == null) {
                request.setResolution(body.reason().trim());
            }
        }
        return requests.saveAndFlush(request);
    }

    private DataSubjectRequest load(UUID id) {
        return requests.findById(id).orElseThrow(() -> notFound("Request " + id));
    }

    private static ApiException notFound(String what) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " does not exist.");
    }

    private Specification<DataSubjectRequest> filter(List<DataSubjectRequestStatus> statuses,
            DataSubjectRequestType type, UUID personId, Boolean overdue) {
        LocalDate today = LocalDate.now(clock);
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (statuses != null && !statuses.isEmpty()) {
                parts.add(root.get("status").in(statuses));
            }
            if (type != null) {
                parts.add(cb.equal(root.get("type"), type));
            }
            if (personId != null) {
                parts.add(cb.equal(root.get("personId"), personId));
            }
            if (Boolean.TRUE.equals(overdue)) {
                parts.add(cb.lessThan(root.get("dueOn"), today));
                parts.add(cb.not(root.get("status").in(FINAL)));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
    }

    private List<DsrResponse> respond(List<DataSubjectRequest> list) {
        LocalDate today = LocalDate.now(clock);
        Map<UUID, UserRef> users = directory.refs(list.stream()
                .flatMap(r -> Stream.of(r.getHandledBy(), r.getCreatedBy())).toList());
        var persons = people.findAll(list.stream().map(DataSubjectRequest::getPersonId)
                .filter(java.util.Objects::nonNull).distinct().toList());
        return list.stream().map(r -> new DsrResponse(r.getId(),
                persons.containsKey(r.getPersonId())
                        ? new PersonRef(r.getPersonId(), persons.get(r.getPersonId()).name()) : null,
                r.getRequesterName(), r.getRequesterEmail(), r.getType(), r.getStatus(), r.getDescription(),
                r.getReceivedAt(), r.getDueOn(), r.getDueOn().isBefore(today) && !FINAL.contains(r.getStatus()),
                r.getResolvedAt(), r.getResolution(), users.get(r.getHandledBy()), r.getCreatedAt(),
                users.getOrDefault(r.getCreatedBy(), UNKNOWN), r.getVersion() == null ? 0 : r.getVersion())).toList();
    }
}
