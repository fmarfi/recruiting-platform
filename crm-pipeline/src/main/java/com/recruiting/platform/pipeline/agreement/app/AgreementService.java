package com.recruiting.platform.pipeline.agreement.app;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.AgreementKind;
import com.recruiting.platform.contracts.AgreementStatus;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Exclusivity;
import com.recruiting.platform.contracts.FeeBasisComponent;
import com.recruiting.platform.contracts.FeeModel;
import com.recruiting.platform.contracts.FeeTrigger;
import com.recruiting.platform.contracts.FileStore;
import com.recruiting.platform.contracts.GuaranteeStartBasis;
import com.recruiting.platform.contracts.InvoiceState;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.domain.Agreement;
import com.recruiting.platform.pipeline.domain.AgreementRepository;
import com.recruiting.platform.pipeline.domain.FeeScheduleItem;
import com.recruiting.platform.pipeline.domain.FeeScheduleItemRepository;
import com.recruiting.platform.pipeline.domain.JobOrder;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Sorts;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.security.AccessRecorder;
import com.recruiting.platform.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

/** Fee agreements (PB-007/008/009): terms of business per account, engagement letters per job order. */
@Service
public class AgreementService {

    static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Logger LOG = LoggerFactory.getLogger(AgreementService.class);
    private static final Map<String, String> SORTABLE = Map.of("title", "title", "status", "status", "createdAt",
            "createdAt", "updatedAt", "updatedAt", "signedOn", "signedOn", "validUntil", "validUntil");

    private final AgreementRepository repo;
    private final FeeScheduleItemRepository items;
    private final JobOrderRefs jobs;
    private final AccountFacts facts;
    private final FileStore store;
    private final AccessRecorder access;
    private final CurrentUser currentUser;

    AgreementService(AgreementRepository repo, FeeScheduleItemRepository items, JobOrderRefs jobs, AccountFacts facts,
            FileStore store, AccessRecorder access, CurrentUser currentUser) {
        this.repo = repo;
        this.items = items;
        this.jobs = jobs;
        this.facts = facts;
        this.store = store;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public Page<Agreement> list(UUID accountId, UUID jobOrderId, List<AgreementStatus> statuses, int page, int size,
            List<String> sort) {
        Specification<Agreement> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (accountId != null) {
                ps.add(cb.equal(root.get("accountId"), accountId));
            }
            if (jobOrderId != null) {
                ps.add(cb.equal(root.get("jobOrderId"), jobOrderId));
            }
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort s = Sorts.parse(sort, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findAll(spec, PageRequest.of(page, size, s));
    }

    @Transactional(readOnly = true)
    public Agreement get(UUID id) {
        return repo.findById(id).orElseThrow(() -> Transitions.notFound("Agreement"));
    }

    @Transactional(readOnly = true)
    public List<FeeScheduleItem> itemsOf(UUID agreementId) {
        return items.findByAgreementIdOrderBySeqAsc(agreementId);
    }

    @Transactional
    public Agreement create(JsonNode json) {
        Body b = new Body(json);
        UUID accountId = required(b.uuid("accountId"), "accountId");
        AgreementKind kind = required(b.enumValue("kind", AgreementKind.class, true), "kind");
        UUID jobOrderId = b.uuid("jobOrderId");
        AccountFacts.Flags account = facts.flags(accountId);
        if (account == null) {
            throw Transitions.notFound("Account");
        }
        if (account.archived()) {
            throw new ApiException(ErrorCode.ARCHIVED, "The account is archived.");
        }
        if (jobOrderId != null) {
            JobOrder job = jobs.require(jobOrderId);
            if (!job.getAccountId().equals(accountId)) {
                throw Transitions.invalid("jobOrderId", "The job order belongs to another account.");
            }
            if (job.getArchivedAt() != null) {
                throw new ApiException(ErrorCode.ARCHIVED, "The job order is archived.");
            }
        }
        if (kind == AgreementKind.ENGAGEMENT_LETTER && jobOrderId == null) {
            throw Transitions.invalid("jobOrderId", "An engagement letter needs a job order.");
        }
        if (kind == AgreementKind.TERMS_OF_BUSINESS && jobOrderId != null) {
            throw Transitions.invalid("jobOrderId", "Terms of business belong to the account, not to a job order.");
        }
        Agreement a = new Agreement();
        a.setAccountId(accountId);
        a.setJobOrderId(jobOrderId);
        a.setKind(kind);
        a.setFeeModel(required(b.enumValue("feeModel", FeeModel.class, true), "feeModel"));
        a.setCurrency(currency(required(b.text("currency", 3, true), "currency")));
        a.setBillingAccountId(accountId);
        apply(a, b);
        a.setStatus(b.has("status") ? b.enumValue("status", AgreementStatus.class, false) : AgreementStatus.DRAFT);
        validateStatus(a);
        validateFee(a);
        List<Body> requested = b.objects("scheduleItems", 20);
        a = repo.saveAndFlush(a);
        List<FeeScheduleItem> saved = replaceItems(a, new ArrayList<>(), requested, billingFlags(a).vatExempt());
        validateItems(a, saved);
        if (a.getStatus() == AgreementStatus.SIGNED) {
            fireSigning(a, saved);
        }
        return a;
    }

    @Transactional
    public Agreement update(UUID id, JsonNode json) {
        Body b = new Body(json);
        Agreement a = get(id);
        Transitions.checkVersion(b.version(), a.getVersion());
        AgreementStatus before = a.getStatus();
        String commercial = commercialSnapshot(a);
        apply(a, b);
        if (b.has("feeModel")) {
            a.setFeeModel(b.enumValue("feeModel", FeeModel.class, false));
        }
        if (b.has("currency")) {
            a.setCurrency(currency(b.text("currency", 3, false)));
        }
        if (b.has("status")) {
            a.setStatus(b.enumValue("status", AgreementStatus.class, false));
        }
        validateStatus(a);
        validateFee(a);
        List<FeeScheduleItem> current = items.findByAgreementIdOrderBySeqAsc(a.getId());
        boolean scheduleChanged = b.has("scheduleItems");
        if (scheduleChanged && current.stream().anyMatch(i -> i.getInvoiceState() != InvoiceState.NOT_DUE)) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "The fee schedule can be replaced only while every item is not yet due.");
        }
        boolean elevated = currentUser.hasAtLeast(Role.MANAGER);
        if (before == AgreementStatus.SIGNED && !elevated
                && (scheduleChanged || !commercial.equals(commercialSnapshot(a)))) {
            throw new ApiException(ErrorCode.FORBIDDEN,
                    "Only a manager or admin may change the commercial terms of a signed agreement.");
        }
        if (scheduleChanged) {
            current = replaceItems(a, current, b.objects("scheduleItems", 20), billingFlags(a).vatExempt());
        }
        validateItems(a, current);
        a = repo.saveAndFlush(a);
        if (before != AgreementStatus.SIGNED && a.getStatus() == AgreementStatus.SIGNED) {
            fireSigning(a, current);
        }
        return a;
    }

    @Transactional
    public void delete(UUID id) {
        Agreement a = get(id);
        if (a.getStatus() != AgreementStatus.DRAFT) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "Only a draft agreement can be deleted; set it to terminated instead.");
        }
        String key = a.getSignedFileId() == null ? null : fileKey(a, a.getSignedFileId());
        items.deleteAll(items.findByAgreementIdOrderBySeqAsc(id));
        items.flush();
        repo.delete(a);
        repo.flush();
        if (key != null) {
            afterCommit(() -> deleteBytes(key));
        }
    }

    @Transactional
    public Agreement uploadSignedFile(UUID id, MultipartFile upload, Long version) {
        Agreement a = get(id);
        Transitions.checkVersion(version, a.getVersion());
        if (upload == null || upload.isEmpty()) {
            throw Transitions.invalid("file", "A non-empty file is required.");
        }
        if (upload.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, "The signed copy may be at most 20 MB.");
        }
        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!isPdf(bytes)) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Only a PDF file is accepted.");
        }
        UUID oldId = a.getSignedFileId();
        UUID fileId = UUID.randomUUID();
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            store.put(fileKey(a, fileId), in, bytes.length, "application/pdf");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        a.setSignedFileId(fileId);
        a.setSignedFileName(cleanName(upload.getOriginalFilename()));
        a.setSignedFileSize((long) bytes.length);
        a.setSignedFileUploadedAt(Instant.now());
        a.setSignedFileUploadedBy(currentUser.id());
        a = repo.saveAndFlush(a);
        if (oldId != null) {
            String oldKey = fileKey(a, oldId);
            afterCommit(() -> deleteBytes(oldKey));
        }
        return a;
    }

    /** Bytes and name of the signed copy; writes the DOWNLOAD access-log row. */
    @Transactional
    public Download download(UUID id) {
        Agreement a = get(id);
        if (a.getSignedFileId() == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "No signed copy was uploaded.");
        }
        access.record(AccessAction.DOWNLOAD, EntityType.AGREEMENT, a.getId());
        try {
            InputStream in = store.get(fileKey(a, a.getSignedFileId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "The file content is missing."));
            try (in) {
                return new Download(a.getSignedFileName(), in.readAllBytes());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Signed copy content. */
    public record Download(String fileName, byte[] bytes) {
    }

    // ---- field binding and rules ----

    private void apply(Agreement a, Body b) {
        if (b.has("title")) {
            a.setTitle(b.text("title", 200, true));
        }
        if (b.has("signedOn")) {
            a.setSignedOn(b.date("signedOn"));
        }
        if (b.has("validFrom")) {
            a.setValidFrom(b.date("validFrom"));
        }
        if (b.has("validUntil")) {
            a.setValidUntil(b.date("validUntil"));
        }
        if (b.has("language")) {
            a.setLanguage(b.enumValue("language", Language.class, true));
        }
        if (b.has("governingLaw")) {
            a.setGoverningLaw(b.text("governingLaw", 100, true));
        }
        if (b.has("exclusivity")) {
            a.setExclusivity(b.enumValue("exclusivity", Exclusivity.class, true));
        }
        if (b.has("feePercent")) {
            a.setFeePercent(b.percent("feePercent"));
        }
        if (b.has("feeBasis")) {
            a.setFeeBasis(new ArrayList<>(
                    b.enumList("feeBasis", FeeBasisComponent.class).stream().map(Enum::name).toList()));
        }
        if (b.has("flatFeeAmount")) {
            a.setFlatFeeAmount(b.money("flatFeeAmount"));
        }
        if (b.has("estimatedFee")) {
            a.setEstimatedFee(b.money("estimatedFee"));
        }
        if (b.has("feeMinimum")) {
            a.setFeeMinimum(b.money("feeMinimum"));
        }
        if (b.has("feeCap")) {
            a.setFeeCap(b.money("feeCap"));
        }
        if (b.has("paymentTermsDays")) {
            a.setPaymentTermsDays(b.integer("paymentTermsDays", 0, 180, false).shortValue());
        }
        if (b.has("guaranteeMonths")) {
            a.setGuaranteeMonths(b.integer("guaranteeMonths", 0, 36, false).shortValue());
        }
        if (b.has("guaranteeStartBasis")) {
            a.setGuaranteeStartBasis(b.enumValue("guaranteeStartBasis", GuaranteeStartBasis.class, false));
        }
        if (b.has("guaranteeTerms")) {
            a.setGuaranteeTerms(b.text("guaranteeTerms", 2000, true));
        }
        if (b.has("expensePolicy")) {
            a.setExpensePolicy(b.text("expensePolicy", 1000, true));
        }
        if (b.has("offLimitsMonths")) {
            Integer m = b.integer("offLimitsMonths", 0, 60, true);
            a.setOffLimitsMonths(m == null ? null : m.shortValue());
        }
        if (b.has("confidentialityTerms")) {
            a.setConfidentialityTerms(b.text("confidentialityTerms", 2000, true));
        }
        if (b.has("notes")) {
            a.setNotes(b.text("notes", 4000, true));
        }
        if (b.has("billingAccountId")) {
            UUID billing = b.uuid("billingAccountId");
            if (billing == null) {
                billing = a.getAccountId();
            }
            if (facts.flags(billing) == null) {
                throw Transitions.notFound("Billing account");
            }
            a.setBillingAccountId(billing);
        }
        if (b.has("signatoryContactId")) {
            UUID contact = b.uuid("signatoryContactId");
            if (contact != null && !facts.isActiveContactOf(contact, a.getAccountId())) {
                throw Transitions.invalid("signatoryContactId",
                        "The signatory must be an active contact of the account.");
            }
            a.setSignatoryContactId(contact);
        }
    }

    private static void validateStatus(Agreement a) {
        if (a.getStatus() == AgreementStatus.SIGNED && a.getSignedOn() == null) {
            throw Transitions.invalid("signedOn", "signedOn is required when the agreement is signed.");
        }
        if (a.getValidFrom() != null && a.getValidUntil() != null && a.getValidUntil().isBefore(a.getValidFrom())) {
            throw Transitions.invalid("validUntil", "validUntil must not be before validFrom.");
        }
    }

    private static void validateFee(Agreement a) {
        if (a.getFeeModel() == FeeModel.FLAT_FEE) {
            if (a.getFlatFeeAmount() == null) {
                throw Transitions.invalid("flatFeeAmount", "A flat fee needs flatFeeAmount.");
            }
        } else {
            if (a.getFeePercent() == null) {
                throw Transitions.invalid("feePercent", "This fee model needs feePercent.");
            }
            if (a.getFeeBasis() == null || a.getFeeBasis().isEmpty()) {
                throw Transitions.invalid("feeBasis", "This fee model needs at least one fee basis component.");
            }
        }
        if (a.getFeeMinimum() != null && a.getFeeCap() != null && a.getFeeMinimum().compareTo(a.getFeeCap()) > 0) {
            throw Transitions.invalid("feeMinimum", "feeMinimum must not be above feeCap.");
        }
    }

    private static void validateItems(Agreement a, List<FeeScheduleItem> list) {
        if (a.getFeeModel() == FeeModel.RETAINED_THIRDS && !list.isEmpty()) {
            BigDecimal sum = list.stream().map(i -> i.getPercent() == null ? BigDecimal.ZERO : i.getPercent())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw Transitions.invalid("scheduleItems", "The item percents of retained thirds must sum to 100.");
            }
        }
    }

    private List<FeeScheduleItem> replaceItems(Agreement a, List<FeeScheduleItem> existing, List<Body> requested,
            boolean vatExempt) {
        Map<UUID, FeeScheduleItem> byId = new HashMap<>();
        existing.forEach(i -> byId.put(i.getId(), i));
        List<UUID> keep = new ArrayList<>();
        for (Body r : requested) {
            UUID itemId = r.uuid("id");
            if (itemId != null) {
                if (!byId.containsKey(itemId)) {
                    throw Transitions.invalid("scheduleItems", "Unknown schedule item id.");
                }
                keep.add(itemId);
            }
        }
        items.deleteAll(existing.stream().filter(i -> !keep.contains(i.getId())).toList());
        items.flush();
        List<FeeScheduleItem> kept = existing.stream().filter(i -> keep.contains(i.getId())).toList();
        for (FeeScheduleItem i : kept) { // park the sequence numbers so a reorder cannot clash on the unique key
            i.setSeq((short) (i.getSeq() + 1000));
        }
        items.saveAllAndFlush(kept);
        List<FeeScheduleItem> out = new ArrayList<>();
        short seq = 1;
        for (Body r : requested) {
            UUID itemId = r.uuid("id");
            FeeScheduleItem item = itemId == null ? new FeeScheduleItem() : byId.get(itemId);
            if (itemId == null) {
                item.setAgreementId(a.getId());
                item.setVatApplies(!vatExempt);
            }
            bindItem(item, r);
            item.setSeq(seq++);
            out.add(item);
        }
        return items.saveAllAndFlush(out);
    }

    private static void bindItem(FeeScheduleItem item, Body r) {
        String label = required(r.text("label", 200, true), "label");
        if (label.isBlank()) {
            throw Transitions.invalid("label", "label must not be empty.");
        }
        item.setLabel(label);
        FeeTrigger trigger = required(r.enumValue("trigger", FeeTrigger.class, true), "trigger");
        item.setTrigger(trigger);
        Integer offset = r.integer("offsetDays", 0, 730, true);
        if (trigger == FeeTrigger.DATE_OFFSET && offset == null) {
            throw Transitions.invalid("offsetDays", "A date-offset item needs offsetDays.");
        }
        item.setOffsetDays(offset == null ? null : offset.shortValue());
        BigDecimal percent = r.percent("percent");
        BigDecimal amount = r.money("amount");
        if ((percent == null) == (amount == null)) {
            throw Transitions.invalid("percent", "Each schedule item needs exactly one of percent and amount.");
        }
        item.setPercent(percent);
        item.setAmount(amount);
        if (r.has("dueDays")) {
            item.setDueDays(r.integer("dueDays", 0, 180, false).shortValue());
        }
        if (r.has("vatApplies")) {
            item.setVatApplies(r.bool("vatApplies", item.isVatApplies()));
        }
    }

    /** SIGNED makes the SIGNING items of an engagement due. Framework items are templates and never fire. */
    private void fireSigning(Agreement a, List<FeeScheduleItem> list) {
        if (a.getJobOrderId() == null) {
            return;
        }
        Instant now = Instant.now();
        LocalDate base = now.atZone(ZoneOffset.UTC).toLocalDate();
        for (FeeScheduleItem i : list) {
            if (i.getTrigger() == FeeTrigger.SIGNING && i.getInvoiceState() == InvoiceState.NOT_DUE
                    && i.getTriggeredAt() == null) {
                i.setInvoiceState(InvoiceState.DUE);
                i.setTriggeredAt(now);
                i.setDueOn(base.plusDays(i.getDueDays()));
            }
        }
        items.saveAllAndFlush(list);
    }

    private AccountFacts.Flags billingFlags(Agreement a) {
        AccountFacts.Flags f = facts.flags(a.getBillingAccountId());
        return f == null ? new AccountFacts.Flags(false, false) : f;
    }

    private static String commercialSnapshot(Agreement a) {
        return String.join("|", String.valueOf(a.getFeeModel()), plain(a.getFeePercent()),
                String.valueOf(a.getFeeBasis()), plain(a.getFlatFeeAmount()), plain(a.getEstimatedFee()),
                plain(a.getFeeMinimum()), plain(a.getFeeCap()), String.valueOf(a.getCurrency()),
                String.valueOf(a.getPaymentTermsDays()), String.valueOf(a.getGuaranteeMonths()),
                String.valueOf(a.getGuaranteeStartBasis()));
    }

    private static String plain(BigDecimal v) {
        return v == null ? "null" : v.stripTrailingZeros().toPlainString();
    }

    private static <T> T required(T v, String field) {
        if (v == null) {
            throw Transitions.invalid(field, field + " is required.");
        }
        return v;
    }

    private static String currency(String c) {
        if (!c.matches("^[A-Z]{3}$")) {
            throw Transitions.invalid("currency", "currency must be a 3-letter upper-case code.");
        }
        return c;
    }

    private static boolean isPdf(byte[] bytes) {
        byte[] magic = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static String cleanName(String original) {
        String n = original == null ? "" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (n.isEmpty()) {
            n = "signed-agreement.pdf";
        }
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }

    private static String fileKey(Agreement a, UUID fileId) {
        return "agreement/" + a.getId() + "/" + fileId;
    }

    private void deleteBytes(String key) {
        try {
            store.delete(key);
        } catch (IOException e) {
            LOG.warn("Could not delete stored bytes {}; the sweep removes them", key);
        }
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
