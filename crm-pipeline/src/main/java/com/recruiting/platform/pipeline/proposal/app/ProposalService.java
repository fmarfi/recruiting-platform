package com.recruiting.platform.pipeline.proposal.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ProposalCreated;
import com.recruiting.platform.contracts.ProposalStatus;
import com.recruiting.platform.pipeline.domain.Proposal;
import com.recruiting.platform.pipeline.domain.ProposalRepository;
import com.recruiting.platform.pipeline.proposal.web.dto.ProposalCreateRequest;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.pipeline.shared.Patch;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.pipeline.shared.Uuids;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read, list and edit proposals (A-05). Terms are editable in DRAFT only. */
@Service
public class ProposalService {

    private final CurrentUser currentUser;
    private static final Map<String, String> SORTABLE = Map.of("title", "title", "status", "status",
            "createdAt", "createdAt", "updatedAt", "updatedAt", "statusChangedAt", "statusChangedAt");
    private static final Pattern AMOUNT = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    private final ProposalRepository repo;
    private final Refs refs;
    private final ApplicationEventPublisher events;

    ProposalService(ProposalRepository repo, Refs refs, ApplicationEventPublisher events, CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.repo = repo;
        this.refs = refs;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<Proposal> list(UUID accountId, List<ProposalStatus> statuses, UUID ownerId, int page, int size,
            List<String> sort) {
        Specification<Proposal> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (accountId != null) {
                ps.add(cb.equal(root.get("accountId"), accountId));
            }
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (ownerId != null) {
                ps.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort s = Sorts.parse(sort, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(spec, PageRequest.of(page, size, s));
    }

    @Transactional(readOnly = true)
    public Proposal get(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Proposal"));
    }

    @Transactional
    public Proposal create(ProposalCreateRequest r) {
        refs.requireAccount(r.accountId());
        UUID owner = r.ownerId() != null ? r.ownerId() : currentUser.id();
        if (r.ownerId() != null) {
            refs.requireUser(owner);
        }
        Proposal p = new Proposal();
        p.setAccountId(r.accountId());
        p.setTitle(r.title().trim());
        p.setTerms(r.terms());
        p.setFeeAmount(r.feeAmount() == null ? null : new BigDecimal(r.feeAmount()));
        p.setCurrency(r.currency());
        p.setOwnerId(owner);
        p.setStatus(ProposalStatus.INITIAL);
        p.setStatusChangedAt(Instant.now());
        repo.saveAndFlush(p);
        events.publishEvent(new ProposalCreated(Uuids.v7(), Instant.now(), currentUser.id(), p.getId(), p.getAccountId(),
                p.getTitle()));
        return p;
    }

    @Transactional
    public Proposal update(UUID id, Patch patch) {
        Proposal p = get(id);
        Transitions.checkVersion(patch.version(), p.getVersion());
        boolean touchesTerms = patch.has("title") || patch.has("terms") || patch.has("feeAmount")
                || patch.has("currency");
        if (touchesTerms && !p.getStatus().termsEditable()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "Title, terms and fee can be changed only while the proposal is a draft.");
        }
        if (patch.has("title")) {
            p.setTitle(patch.text("title", 1, 200, false).trim());
        }
        if (patch.has("terms")) {
            p.setTerms(patch.text("terms", 0, 10000, true));
        }
        if (patch.has("feeAmount")) {
            String amount = patch.text("feeAmount", 0, 20, true);
            if (amount != null && !AMOUNT.matcher(amount).matches()) {
                throw Transitions.invalid("feeAmount", "feeAmount must be a decimal with up to 2 digits.");
            }
            p.setFeeAmount(amount == null ? null : new BigDecimal(amount));
        }
        if (patch.has("currency")) {
            String currency = patch.text("currency", 0, 3, true);
            if (currency != null && !CURRENCY.matcher(currency).matches()) {
                throw Transitions.invalid("currency", "currency must be a 3-letter upper-case code.");
            }
            p.setCurrency(currency);
        }
        if (patch.has("ownerId")) {
            UUID owner = patch.uuid("ownerId");
            refs.requireUser(owner);
            p.setOwnerId(owner);
        }
        return repo.saveAndFlush(p);
    }
}
