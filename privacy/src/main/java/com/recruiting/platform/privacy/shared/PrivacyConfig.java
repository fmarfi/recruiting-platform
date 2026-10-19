package com.recruiting.platform.privacy.shared;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Privacy wiring: one {@link Clock} (tests replace it to shift time) and the nightly scheduler. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class PrivacyConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
