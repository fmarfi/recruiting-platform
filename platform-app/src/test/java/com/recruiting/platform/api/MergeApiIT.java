package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.accounts.PersonMergeApi;
import com.recruiting.platform.contracts.PersonMerged;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.pipeline.PipelinePersonApi;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Merge orchestration: order of steps, privacy part (consent, requests, DSRs, retention), event, roles. */
class MergeApiIT extends AbstractApiIT {

        @MockitoBean
    private PersonMergeApi accountsApi;

    @MockitoBean
    private PipelinePersonApi pipelineApi;

    @Autowired
    private JdbcTemplate jdbc;

    private String body(UUID merged) {
        return "{\"mergedPersonId\":\"" + merged + "\",\"takeFromMerged\":[\"phone\"],\"version\":0}";
    }

    @Test
    void mergeRunsTheStepsInOrderAndMovesConsentAndRequests() throws Exception {
        Client m = login(Role.MANAGER);
        UUID survivor = PrivacyTestSupport.insertPerson(jdbc, m.userId(), "Survivor Sam", "sam-" + UUID.randomUUID() + "@example.test");
        UUID merged = PrivacyTestSupport.insertPerson(jdbc, m.userId(), "Merged Mia", "mia-" + UUID.randomUUID() + "@example.test");
        PrivacyTestSupport.insertPrivacy(jdbc, survivor, LocalDate.now().minusMonths(10), LocalDate.now().plusMonths(14));
        PrivacyTestSupport.insertPrivacy(jdbc, merged, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(22));
        UUID notice = jdbc.queryForObject("select id from privacy_notice limit 1", UUID.class);
        // the survivor refused long ago, the merged person gave consent later: GIVEN wins (latest decision)
        jdbc.update("insert into consent_record (id, person_id, decision, channel, notice_id, recorded_at) "
                + "values (?, ?, 'REFUSED', 'PHONE', ?, now() - interval '300 days')", UUID.randomUUID(), survivor, notice);
        jdbc.update("insert into consent_record (id, person_id, decision, channel, notice_id, recorded_at) "
                + "values (?, ?, 'GIVEN', 'PHONE', ?, now() - interval '20 days')", UUID.randomUUID(), merged, notice);
        jdbc.update("update person_privacy set consent_status = 'REFUSED' where person_id = ?", survivor);
        jdbc.update("update person_privacy set consent_status = 'GIVEN' where person_id = ?", merged);
        UUID dsr = UUID.randomUUID();
        jdbc.update("insert into data_subject_request (id, person_id, requester_name, requester_email, type, status, "
                + "received_at, due_on, version) values (?, ?, 'x', 'x@example.test', 'ACCESS', 'RECEIVED', now(), "
                + "current_date + 30, 0)", dsr, merged);
        Long rev = jdbc.queryForObject("select nextval('revinfo_seq')", Long.class);
        jdbc.update("insert into revinfo (rev, revtstmp) values (?, 5)", rev);
        jdbc.update("insert into data_subject_request_aud (id, rev, revtype, person_id) values (?, ?, 0, ?)", dsr, rev, merged);

        mvc.perform(m.with(post("/api/v1/people/" + survivor + "/merge")).contentType(MediaType.APPLICATION_JSON)
                .content(body(merged))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Survivor Sam"));

        InOrder order = inOrder(accountsApi, pipelineApi);
        order.verify(accountsApi).mergeProfile(eq(survivor), eq(merged), eq(java.util.Set.of("phone")), eq(0L), any());
        order.verify(pipelineApi).mergePerson(survivor, merged);
        order.verify(accountsApi).finish(eq(survivor), eq(merged), any(), any());
        assertThat(jdbc.queryForObject("select consent_status from person_privacy where person_id = ?", String.class,
                survivor)).isEqualTo("GIVEN");
        assertThat(jdbc.queryForObject("select count(*) from consent_record where person_id = ?", Integer.class,
                survivor)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from person_privacy where person_id = ?", Integer.class,
                merged)).isZero();
        assertThat(jdbc.queryForObject("select person_id from data_subject_request where id = ?", UUID.class, dsr))
                .isEqualTo(survivor);
        assertThat(jdbc.queryForObject("select last_consent_at is not null from person_privacy where person_id = ?",
                Boolean.class, survivor)).isTrue();
    }

    @Test
    void consultantsAreForbiddenAndBadMergesAreRejectedBeforeAnyStep() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        Client manager = login(Role.MANAGER);
        UUID a = PrivacyTestSupport.insertPerson(jdbc, manager.userId(), "Solo Sia", null);
        mvc.perform(consultant.with(post("/api/v1/people/" + a + "/merge")).contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID()))).andExpect(status().isForbidden());
        mvc.perform(manager.with(post("/api/v1/people/" + a + "/merge")).contentType(MediaType.APPLICATION_JSON)
                .content(body(a))).andExpect(status().isConflict());
        mvc.perform(manager.with(post("/api/v1/people/" + a + "/merge")).contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(manager.with(post("/api/v1/people/" + a + "/merge")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mergedPersonId\":\"" + UUID.randomUUID() + "\"}")).andExpect(status().isBadRequest());
        verify(accountsApi, never()).mergeProfile(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }
}
