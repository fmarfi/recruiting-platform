package com.recruiting.platform.pipeline.application.app;

import com.recruiting.platform.accounts.OffLimitsApi;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.contracts.ShareBlockReason;
import com.recruiting.platform.pipeline.ShareGuard;
import com.recruiting.platform.pipeline.shared.Transitions;
import com.recruiting.platform.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The two guards of ADR 0011: the sharing guard (consent, no override) and the off-limits guard (manager override).
 * Both SPIs are optional beans so the module starts without their implementations; a missing {@link ShareGuard}
 * fails closed (CONSENT_REQUIRED), a missing {@link OffLimitsApi} finds no rule.
 */
@Component
public class ApplicationGuards {

    private final ObjectProvider<ShareGuard> shareGuard;
    private final ObjectProvider<OffLimitsApi> offLimits;
    private final CurrentUser currentUser;

    ApplicationGuards(ObjectProvider<ShareGuard> shareGuard, ObjectProvider<OffLimitsApi> offLimits,
            CurrentUser currentUser) {
        this.shareGuard = shareGuard;
        this.offLimits = offLimits;
        this.currentUser = currentUser;
    }

    /**
     * Validates an override reason given in a request: only MANAGER/ADMIN may send one (403 otherwise), 3 to 500
     * characters after trimming.
     *
     * @param raw the reason from the request, may be {@code null} or blank
     * @return the trimmed reason, or {@code null} when none was sent
     */
    public String checkOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (!currentUser.hasAtLeast(Role.MANAGER)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only a manager or admin may override an off-limits rule.");
        }
        String t = raw.trim();
        if (t.length() < 3 || t.length() > 500) {
            throw Transitions.invalid("offLimitsOverrideReason", "offLimitsOverrideReason must be 3 to 500 characters.");
        }
        return t;
    }

    /**
     * Sharing guard: 409 CONSENT_REQUIRED unless the person is shareable. Not overridable.
     *
     * @param personId the person
     */
    public void requireShareable(UUID personId) {
        ShareGuard guard = shareGuard.getIfAvailable();
        ShareGuard.ShareDecision d = guard == null
                ? ShareGuard.ShareDecision.blocked(ShareBlockReason.CONSENT_NOT_REQUESTED) : guard.check(personId);
        if (!d.shareable()) {
            throw new ApiException(ErrorCode.CONSENT_REQUIRED,
                    "This person has not given consent yet. Send the consent request first.",
                    Map.of("blockingItems", List.of(Map.of("id", personId.toString(), "type", "PERSON",
                            "reasonCode", d.reason().name()))));
        }
    }

    /**
     * Off-limits guard: 409 OFF_LIMITS when a rule matches, unless an override is stored or given.
     *
     * @param personId   the person
     * @param accountId  account of the job order (its own rules are ignored)
     * @param overridden whether an override reason is already stored or sent now
     */
    public void requireNotOffLimits(UUID personId, UUID accountId, boolean overridden) {
        if (overridden) {
            return;
        }
        OffLimitsApi api = offLimits.getIfAvailable();
        if (api == null) {
            return;
        }
        List<OffLimitsApi.Match> matches = api.findForPerson(personId, accountId);
        if (!matches.isEmpty()) {
            List<Map<String, Object>> items = matches.stream().map(m -> {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("id", m.ruleId().toString());
                item.put("type", "OFF_LIMITS_RULE");
                if (m.companyName() != null) {
                    item.put("title", m.companyName());
                }
                item.put("reasonCode", m.reason().name());
                return item;
            }).toList();
            throw new ApiException(ErrorCode.OFF_LIMITS,
                    "The person works at an off-limits company. A manager may add them with a reason.",
                    Map.of("blockingItems", items));
        }
    }

    /** One field problem of the shortlist guard. */
    public record FieldIssue(String field, String code, String message) {
    }

    /**
     * The shortlist guard of ADR 0013: a rating (1-5) and a rationale of at least 20 characters.
     *
     * @param rating    the rating the application would have
     * @param rationale the rationale it would have
     * @return the problems, empty when the application may be on the shortlist
     */
    public static List<FieldIssue> shortlistIssues(Short rating, String rationale) {
        List<FieldIssue> out = new java.util.ArrayList<>();
        if (rating == null) {
            out.add(new FieldIssue("rating", "REQUIRED", "A rating from 1 to 5 is required on the shortlist."));
        }
        String r = rationale == null ? "" : rationale.trim();
        if (r.isEmpty()) {
            out.add(new FieldIssue("rationale", "REQUIRED", "A rationale is required on the shortlist."));
        } else if (r.length() < 20) {
            out.add(new FieldIssue("rationale", "SIZE", "The rationale needs at least 20 characters."));
        }
        return out;
    }

    /** The 409 of the shortlist guard, with the field errors in {@code errors}. */
    public static ApiException shortlistFailed(Enum<?> from, Enum<?> to, List<FieldIssue> issues) {
        List<Map<String, String>> errors = issues.stream()
                .map(i -> Map.of("field", i.field(), "code", i.code(), "message", i.message())).toList();
        return new ApiException(ErrorCode.TRANSITION_GUARD_FAILED,
                "A rating and a rationale of at least 20 characters are needed on the shortlist.",
                Map.of("currentStatus", from.name(), "requestedStatus", to.name(), "errors", errors));
    }
}
