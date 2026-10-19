package com.recruiting.platform.privacy.consent.web;

import com.recruiting.platform.contracts.EmailDeliveryStatus;
import com.recruiting.platform.privacy.consent.app.ConsentBulkService;
import com.recruiting.platform.privacy.consent.app.ConsentMapper;
import com.recruiting.platform.privacy.consent.app.ConsentQueryService;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.BulkResult;
import com.recruiting.platform.privacy.consent.web.dto.ConsentDtos.ConsentRequestResponse;
import com.recruiting.platform.privacy.consent.web.dto.ConsentRequests.BulkCreate;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.ApiPageable;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tag Consent, across people: the list of consent e-mails and the campaign (managers and admins). */
@RestController
@RequestMapping("/api/v1/consent-requests")
@PreAuthorize("hasRole('MANAGER')")
class ConsentRequestController {

    private static final Map<String, String> SORTABLE = Map.of("queuedAt", "queuedAt", "sentAt", "sentAt",
            "expiresAt", "expiresAt", "respondedAt", "respondedAt");

    private final ConsentQueryService queries;
    private final ConsentBulkService bulk;
    private final ConsentMapper mapper;

    ConsentRequestController(ConsentQueryService queries, ConsentBulkService bulk, ConsentMapper mapper) {
        this.queries = queries;
        this.bulk = bulk;
        this.mapper = mapper;
    }

    @GetMapping
    ApiPage<ConsentRequestResponse> list(@RequestParam(required = false) UUID personId,
            @RequestParam(required = false) List<EmailDeliveryStatus> deliveryStatus,
            @RequestParam(required = false) Boolean answered, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> sort) {
        var pageable = ApiPageable.of(page, size, sort, SORTABLE, Sort.by(Sort.Direction.DESC, "queuedAt"));
        Page<ConsentRequest> result = queries.requests(personId, deliveryStatus, answered, pageable);
        List<ConsentRequestResponse> mapped = mapper.requests(result.getContent());
        return new ApiPage<>(mapped, new ApiPage.PageMeta(result.getSize(), result.getNumber(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.ACCEPTED)
    BulkResult bulk(@Valid @RequestBody BulkCreate body) {
        return bulk.send(body);
    }
}
