package com.recruiting.platform.pipeline.agreement.web.dto;

import com.recruiting.platform.contracts.AgreementKind;
import com.recruiting.platform.contracts.AgreementStatus;
import com.recruiting.platform.contracts.Exclusivity;
import com.recruiting.platform.contracts.FeeModel;
import com.recruiting.platform.contracts.FeeTrigger;
import com.recruiting.platform.contracts.GuaranteeStartBasis;
import com.recruiting.platform.contracts.InvoiceState;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.pipeline.shared.dto.AccountRef;
import com.recruiting.platform.pipeline.shared.dto.JobOrderRef;
import com.recruiting.platform.pipeline.shared.dto.PersonRef;
import com.recruiting.platform.pipeline.shared.dto.UserRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** OpenAPI Agreement. Money and percent values are strings. */
public record AgreementResponse(UUID id, AccountRef account, JobOrderRef jobOrder, AgreementKind kind,
        AgreementStatus status, String title, LocalDate signedOn, LocalDate validFrom, LocalDate validUntil,
        Language language, String governingLaw, Exclusivity exclusivity, FeeModel feeModel, String feePercent,
        List<String> feeBasis, String flatFeeAmount, String estimatedFee, String feeMinimum, String feeCap,
        String currency, int paymentTermsDays, int guaranteeMonths, GuaranteeStartBasis guaranteeStartBasis,
        String guaranteeTerms, String expensePolicy, Integer offLimitsMonths, String confidentialityTerms,
        PersonRef signatory, UUID signatoryContactId, AccountRef billingAccount, UUID billingAccountId,
        String notes, SignedFile signedFile, List<Item> scheduleItems, Instant createdAt, Instant updatedAt,
        Long version) {

    /** The signed copy. */
    public record SignedFile(UUID id, String fileName, long sizeBytes, Instant uploadedAt, UserRef uploadedBy) {
    }

    /** OpenAPI FeeScheduleItem. */
    public record Item(UUID id, UUID agreementId, AccountRef account, JobOrderRef jobOrder, int seq, String label,
            FeeTrigger trigger, Integer offsetDays, String percent, String amount, int dueDays, boolean vatApplies,
            String computedAmount, String currency, InvoiceState invoiceState, Instant triggeredAt, LocalDate dueOn,
            String einvoiceRef, LocalDate invoicedOn, String invoicedAmount, LocalDate paidOn, String stateReason,
            Long version) {
    }
}
