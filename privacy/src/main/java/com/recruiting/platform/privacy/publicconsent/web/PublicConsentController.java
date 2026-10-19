package com.recruiting.platform.privacy.publicconsent.web;

import com.recruiting.platform.privacy.publicconsent.app.PublicConsentService;
import com.recruiting.platform.privacy.publicconsent.web.dto.PublicConsentDtos.PublicConsentView;
import com.recruiting.platform.privacy.publicconsent.web.dto.PublicConsentDtos.PublicDecision;
import com.recruiting.platform.security.RequestRateLimiter;
import com.recruiting.platform.security.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tag Public consent: no login, rate-limited by IP. The token is never logged or stored in the access log. */
@RestController
@RequestMapping("/api/v1/public/consent/{token}")
class PublicConsentController {

    private static final int PER_MINUTE = 60;

    private final PublicConsentService service;
    private final RequestRateLimiter limiter;

    PublicConsentController(PublicConsentService service, RequestRateLimiter limiter) {
        this.service = service;
        this.limiter = limiter;
    }

    @GetMapping
    PublicConsentView view(@PathVariable String token, HttpServletRequest request) {
        limit(request);
        return service.view(token);
    }

    @PostMapping
    PublicConsentView decide(@PathVariable String token, @Valid @RequestBody PublicDecision body,
            HttpServletRequest request) {
        limit(request);
        return service.decide(token, body.decision(), request.getRemoteAddr());
    }

    private void limit(HttpServletRequest request) {
        if (!limiter.tryAcquire("public-consent:" + request.getRemoteAddr(), PER_MINUTE, Duration.ofMinutes(1))) {
            throw new TooManyRequestsException();
        }
    }
}
