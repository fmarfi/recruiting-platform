package com.recruiting.platform.privacy.consent.web;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.privacy.consent.app.ConsentMapper;
import com.recruiting.platform.privacy.consent.app.ConsentQueryService;
import com.recruiting.platform.privacy.consent.app.ConsentRequestService;
import com.recruiting.platform.privacy.consent.app.ConsentService;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentOverviewResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRecordResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRequestResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentRequests.RecordCreate;
import com.recruiting.platform.privacy.consent.web.dto.ConsentRequests.RequestCreate;
import com.recruiting.platform.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tag Consent, per person: overview, send the notice and consent e-mail, record a decision. */
@RestController
@RequestMapping("/api/v1/people/{id}/consent")
@PreAuthorize("hasRole('CONSULTANT')")
class ConsentController {

    private static final Set<ConsentChannel> MANUAL = Set.of(ConsentChannel.IN_PERSON, ConsentChannel.PHONE,
            ConsentChannel.IMPORT);

    private final ConsentQueryService queries;
    private final ConsentRequestService requests;
    private final ConsentService consent;
    private final ConsentMapper mapper;
    private final CurrentUser currentUser;

    ConsentController(ConsentQueryService queries, ConsentRequestService requests, ConsentService consent,
            ConsentMapper mapper, CurrentUser currentUser) {
        this.queries = queries;
        this.requests = requests;
        this.consent = consent;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @GetMapping
    ConsentOverviewResponse overview(@PathVariable UUID id) {
        return queries.overview(id);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ConsentRequestResponse send(@PathVariable UUID id, @RequestBody(required = false) RequestCreate body) {
        var saved = requests.send(id, body == null ? null : body.language(), currentUser.id());
        return mapper.requests(List.of(saved)).get(0);
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    ConsentRecordResponse record(@PathVariable UUID id, @Valid @RequestBody RecordCreate body) {
        if (!MANUAL.contains(body.channel())) {
            throw new ApiException(com.recruiting.platform.contracts.ErrorCode.VALIDATION_FAILED,
                    "Channel must be IN_PERSON, PHONE or IMPORT.",
                    Map.of("errors", List.of(Map.of("field", "channel", "code", "INVALID",
                            "message", "Only IN_PERSON, PHONE or IMPORT can be recorded by hand."))));
        }
        var saved = consent.record(new ConsentService.Decision(id, body.decision(), body.channel(),
                body.noticeId(), null, body.recordedAt(), currentUser.id(), body.evidence(), null, false));
        return mapper.records(List.of(saved)).get(0);
    }
}
