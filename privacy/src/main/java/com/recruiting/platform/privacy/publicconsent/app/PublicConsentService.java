package com.recruiting.platform.privacy.publicconsent.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ConsentChannel;
import com.recruiting.platform.contracts.ConsentDecision;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.privacy.consent.app.ConsentService;
import com.recruiting.platform.privacy.domain.ConsentRequest;
import com.recruiting.platform.privacy.domain.ConsentRequestRepository;
import com.recruiting.platform.privacy.domain.PersonPrivacyRepository;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import com.recruiting.platform.privacy.person.app.PersonGateway;
import com.recruiting.platform.privacy.person.app.PersonInfo;
import com.recruiting.platform.privacy.publicconsent.web.dto.PublicConsentDtos.PublicConsentView;
import com.recruiting.platform.privacy.shared.ConsentTokens;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The person's side of S-01: look at the notice and give, refuse or withdraw consent through the one-time link.
 * The token stays usable until it expires so the person can withdraw later. Unknown token: 404; expired: 410.
 */
@Service
public class PublicConsentService {

    private final ConsentRequestRepository requests;
    private final PrivacyNoticeRepository notices;
    private final PersonPrivacyRepository privacy;
    private final PersonGateway people;
    private final ConsentService consent;
    private final Clock clock;
    private final String controllerName;

    PublicConsentService(ConsentRequestRepository requests, PrivacyNoticeRepository notices,
            PersonPrivacyRepository privacy, PersonGateway people, ConsentService consent, Clock clock,
            @Value("${app.privacy.controller-name:Executive Search Co.}") String controllerName) {
        this.requests = requests;
        this.notices = notices;
        this.privacy = privacy;
        this.people = people;
        this.consent = consent;
        this.clock = clock;
        this.controllerName = controllerName;
    }

    @Transactional(readOnly = true)
    public PublicConsentView view(String token) {
        ConsentRequest request = valid(token);
        return render(request, person(request));
    }

    @Transactional
    public PublicConsentView decide(String token, ConsentDecision decision, String ipAddress) {
        ConsentRequest request = valid(token);
        PersonInfo person = person(request);
        ConsentStatus current = privacy.findById(request.getPersonId()).map(p -> p.getConsentStatus())
                .orElse(ConsentStatus.NOT_SENT);
        // Withdrawing something that was never given is a refusal.
        ConsentDecision effective = decision == ConsentDecision.WITHDRAWN && current != ConsentStatus.GIVEN
                ? ConsentDecision.REFUSED : decision;
        consent.record(new ConsentService.Decision(request.getPersonId(), effective, ConsentChannel.EMAIL_LINK,
                request.getNoticeId(), request.getId(), null, null, null, ipAddress, true));
        return render(request, person);
    }

    private ConsentRequest valid(String token) {
        ConsentRequest request = requests.findByTokenHash(ConsentTokens.hash(token))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "This link is not valid."));
        if (clock.instant().isAfter(request.getExpiresAt())) {
            throw new ApiException(ErrorCode.GONE, "This link has expired. Ask us to send a new one.");
        }
        return request;
    }

    private PersonInfo person(ConsentRequest request) {
        return people.find(request.getPersonId()).filter(p -> !p.anonymised())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "This link is not valid."));
    }

    private PublicConsentView render(ConsentRequest request, PersonInfo person) {
        PrivacyNotice notice = notices.findById(request.getNoticeId()).orElseThrow();
        ConsentStatus status = privacy.findById(request.getPersonId()).map(p -> p.getConsentStatus())
                .orElse(ConsentStatus.SENT);
        return new PublicConsentView(request.getLanguage(), notice.getVersion(), notice.getNoticeText(),
                notice.getConsentText(), person.firstName(), controllerName, status, request.getExpiresAt());
    }
}
