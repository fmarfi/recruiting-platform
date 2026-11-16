package com.recruiting.platform.pipeline.agreement.app;

import com.recruiting.platform.contracts.FeeModel;
import com.recruiting.platform.pipeline.agreement.web.dto.AgreementResponse;
import com.recruiting.platform.pipeline.agreement.web.dto.AgreementResponse.Item;
import com.recruiting.platform.pipeline.agreement.web.dto.AgreementResponse.SignedFile;
import com.recruiting.platform.pipeline.domain.Agreement;
import com.recruiting.platform.pipeline.domain.FeeScheduleItem;
import com.recruiting.platform.pipeline.domain.FeeScheduleItemRepository;
import com.recruiting.platform.pipeline.shared.JobOrderRefs;
import com.recruiting.platform.pipeline.shared.Refs;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Builds agreement responses with batched account, person, user, job order and schedule lookups. */
@Component
public class AgreementMapper {

    private final Refs refs;
    private final JobOrderRefs jobs;
    private final AccountFacts facts;
    private final FeeScheduleItemRepository items;

    AgreementMapper(Refs refs, JobOrderRefs jobs, AccountFacts facts, FeeScheduleItemRepository items) {
        this.refs = refs;
        this.jobs = jobs;
        this.facts = facts;
        this.items = items;
    }

    public AgreementResponse toResponse(Agreement a) {
        return toResponses(List.of(a)).get(0);
    }

    @Transactional(readOnly = true)
    public List<AgreementResponse> toResponses(List<Agreement> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Collection<UUID> accountIds = new HashSet<>();
        Collection<UUID> contactIds = new HashSet<>();
        Collection<UUID> userIds = new HashSet<>();
        for (Agreement a : list) {
            accountIds.add(a.getAccountId());
            accountIds.add(a.getBillingAccountId());
            if (a.getSignatoryContactId() != null) {
                contactIds.add(a.getSignatoryContactId());
            }
            if (a.getSignedFileUploadedBy() != null) {
                userIds.add(a.getSignedFileUploadedBy());
            }
        }
        Map<UUID, UUID> personByContact = facts.personIds(contactIds);
        Refs.Lookup l = refs.load(accountIds, personByContact.values(), userIds);
        Map<UUID, JobOrderRef> jobRefs = jobs.load(list.stream().map(Agreement::getJobOrderId).toList());
        Map<UUID, List<FeeScheduleItem>> itemsByAgreement = items
                .findByAgreementIdInOrderBySeqAsc(list.stream().map(Agreement::getId).toList()).stream()
                .collect(Collectors.groupingBy(FeeScheduleItem::getAgreementId));
        List<AgreementResponse> out = new ArrayList<>();
        for (Agreement a : list) {
            JobOrderRef job = a.getJobOrderId() == null ? null : jobRefs.get(a.getJobOrderId());
            List<Item> mapped = itemsByAgreement.getOrDefault(a.getId(), List.of()).stream()
                    .map(i -> item(a, i, l, job)).toList();
            SignedFile file = a.getSignedFileId() == null ? null
                    : new SignedFile(a.getSignedFileId(), a.getSignedFileName(), a.getSignedFileSize(),
                            a.getSignedFileUploadedAt(), l.user(a.getSignedFileUploadedBy()));
            out.add(new AgreementResponse(a.getId(), l.account(a.getAccountId()), job, a.getKind(), a.getStatus(),
                    a.getTitle(), a.getSignedOn(), a.getValidFrom(), a.getValidUntil(), a.getLanguage(),
                    a.getGoverningLaw(), a.getExclusivity(), a.getFeeModel(), str(a.getFeePercent()),
                    a.getFeeBasis() == null ? List.of() : a.getFeeBasis(), str(a.getFlatFeeAmount()),
                    str(a.getEstimatedFee()), str(a.getFeeMinimum()), str(a.getFeeCap()), a.getCurrency(),
                    a.getPaymentTermsDays(), a.getGuaranteeMonths(), a.getGuaranteeStartBasis(),
                    a.getGuaranteeTerms(), a.getExpensePolicy(), a.getOffLimitsMonths() == null ? null
                            : a.getOffLimitsMonths().intValue(),
                    a.getConfidentialityTerms(), l.personRef(personByContact.get(a.getSignatoryContactId())),
                    a.getSignatoryContactId(), l.account(a.getBillingAccountId()), a.getBillingAccountId(),
                    a.getNotes(), file, mapped, a.getCreatedAt(), a.getUpdatedAt(), a.getVersion()));
        }
        return out;
    }

    private static Item item(Agreement a, FeeScheduleItem i, Refs.Lookup l, JobOrderRef job) {
        return new Item(i.getId(), a.getId(), l.account(a.getAccountId()), job, i.getSeq(), i.getLabel(),
                i.getTrigger(), i.getOffsetDays() == null ? null : i.getOffsetDays().intValue(), str(i.getPercent()),
                str(i.getAmount()), i.getDueDays(), i.isVatApplies(), computed(a, i), a.getCurrency(),
                i.getInvoiceState(), i.getTriggeredAt(), i.getDueOn(), i.getEinvoiceRef(), i.getInvoicedOn(),
                str(i.getInvoicedAmount()), i.getPaidOn(), i.getStateReason(), i.getVersion());
    }

    /** The amount, or the percent of the estimated fee (flat fee for FLAT_FEE); null when not computable yet. */
    private static String computed(Agreement a, FeeScheduleItem i) {
        if (i.getAmount() != null) {
            return i.getAmount().toPlainString();
        }
        BigDecimal base = a.getFeeModel() == FeeModel.FLAT_FEE ? a.getFlatFeeAmount() : a.getEstimatedFee();
        if (base == null || i.getPercent() == null) {
            return null;
        }
        return base.multiply(i.getPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static String str(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }
}
