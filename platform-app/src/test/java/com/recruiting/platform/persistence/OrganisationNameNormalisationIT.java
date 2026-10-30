package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** R63 (schema.md "Organisation identity"): normalize_org_name, normalize_org_names and the unique index. */
class OrganisationNameNormalisationIT extends AbstractPersistenceIT {

    @Autowired
    private JdbcTemplate jdbc;

    private String norm(String name) {
        return jdbc.queryForObject("SELECT normalize_org_name(?)", String.class, name);
    }

    @Test
    void stripsLegalFormsAndFoldsTurkishLetters() {
        assertThat(norm("Örnek Gıda Sanayi ve Ticaret A.Ş.")).isEqualTo("ornek gida");
        assertThat(norm("ACME Ltd. Şti.")).isEqualTo(norm("Acme")).isEqualTo("acme");
        assertThat(norm("Marmara Holding")).isEqualTo("marmara holding");
        assertThat(norm("Gıda")).isEqualTo("gida");
        assertThat(norm("İstanbul GIDA A.Ş.")).isEqualTo("istanbul gida");
    }

    @Test
    void anEmptyResultFallsBackToTheCollapsedName() {
        assertThat(norm("A.Ş.")).isEqualTo("a s");
    }

    @Test
    void arrayVersionNormalisesEachElement() {
        assertThat(jdbc.queryForObject("SELECT normalize_org_names(ARRAY['Örnek A.Ş.', 'ACME Ltd.'])::text",
                String.class)).isEqualTo("{ornek,acme}");
        assertThat(jdbc.queryForObject("SELECT normalize_org_names('{}'::text[])::text", String.class))
                .isEqualTo("{}");
    }

    @Test
    void theUniqueIndexRejectsASecondActiveOrganisationWithTheSameNormalisedName() {
        UUID user = newUser().getId();
        String base = "Örnek Gıda " + UUID.randomUUID().toString().replace("-", "").replaceAll("[0-9]", "x");
        insert(base + " A.Ş.", user, false);
        assertThatThrownBy(() -> insert(base.toUpperCase() + " Sanayi ve Ticaret Anonim Şirketi", user, false))
                .isInstanceOf(DataIntegrityViolationException.class);
        // an archived row may share the name
        insert(base + " Ltd.", user, true);
    }

    private void insert(String name, UUID owner, boolean archived) {
        String tail = archived ? "now(), ?, 'DUPLICATE'" : "null, null, null";
        Object[] args = archived
                ? new Object[] {UUID.randomUUID(), name, owner, owner}
                : new Object[] {UUID.randomUUID(), name, owner};
        jdbc.update("INSERT INTO account (id, name, sector, owner_id, is_prospect, archived_at, archived_by, "
                + "archive_reason) VALUES (?, ?, 'Food', ?, true, " + tail + ")", args);
    }
}
