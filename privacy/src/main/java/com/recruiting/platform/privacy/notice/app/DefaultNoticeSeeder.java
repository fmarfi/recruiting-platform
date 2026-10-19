package com.recruiting.platform.privacy.notice.app;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.privacy.domain.PrivacyNotice;
import com.recruiting.platform.privacy.domain.PrivacyNoticeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Makes sure every language has a notice (NFR-01: everyone gets an information notice). When a language has none,
 * the bundled draft text, version "2026-10", is published. Admins replace it with a lawyer-approved version later.
 */
@Component
class DefaultNoticeSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNoticeSeeder.class);
    static final String VERSION = "2026-10";

    private final PrivacyNoticeRepository notices;
    private final Clock clock;
    private final String controller;

    DefaultNoticeSeeder(PrivacyNoticeRepository notices, Clock clock,
            @Value("${app.privacy.controller-name:Executive Search Co.}") String controller) {
        this.notices = notices;
        this.clock = clock;
        this.controller = controller;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Language language : Language.values()) {
            if (!notices.existsByLanguage(language)) {
                String code = language.name().toLowerCase();
                PrivacyNotice notice = new PrivacyNotice();
                notice.setVersion(VERSION);
                notice.setLanguage(language);
                notice.setNoticeText(read("notice-" + code + ".md"));
                notice.setConsentText(read("consent-" + code + ".md"));
                notice.setEffectiveFrom(LocalDate.now(clock).minusDays(1));
                notices.save(notice);
                LOG.info("Published default {} privacy notice {}", language, VERSION);
            }
        }
    }

    private String read(String name) throws IOException {
        try (InputStream in = new ClassPathResource("privacy/notices/" + name).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("{controller}", controller);
        }
    }
}
