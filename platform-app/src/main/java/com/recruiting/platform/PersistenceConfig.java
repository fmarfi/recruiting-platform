package com.recruiting.platform;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Persistence wiring shared by all modules: Spring Data auditing (created/updated columns) and the Envers
 * settings that the Flyway schema (V202610160002 and later) is built for.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!test") // the test profile runs without a database (see application-test.yml)
@EnableJpaAuditing
class PersistenceConfig {

    @Bean
    HibernatePropertiesCustomizer enversProperties() {
        return props -> {
            props.put("org.hibernate.envers.audit_table_suffix", "_aud");
            props.put("org.hibernate.envers.audit_strategy", "org.hibernate.envers.strategy.ValidityAuditStrategy");
        };
    }
}
