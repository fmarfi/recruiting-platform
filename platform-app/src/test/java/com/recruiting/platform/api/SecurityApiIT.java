package com.recruiting.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.domain.AccessLogRepository;
import com.recruiting.platform.security.domain.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

/** Login, session, CSRF, roles, lock-out, users and audit endpoints through the real filter chain. */
class SecurityApiIT extends AbstractApiIT {

    @Autowired
    private AccessLogRepository accessLog;

    @Test
    void everyApiPathNeedsALoginAndAnswersWithAProblem() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/v1/some/unknown/path")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsOpenWithoutDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void loginStartsASessionWritesTheAccessLogAndSetsTheCsrfCookie() throws Exception {
        AppUser user = createUser(Role.CONSULTANT);
        var first = mvc.perform(get("/api/v1/auth/me")).andReturn();
        assertThat(first.getResponse().getCookie("XSRF-TOKEN")).isNotNull();

        Client client = login(user);
        mvc.perform(get("/api/v1/auth/me").session(client.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("CONSULTANT"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        var rows = accessLog.findByUserIdOrderByAtDesc(user.getId(), PageRequest.of(0, 5)).getContent();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAction()).isEqualTo(AccessAction.VIEW);
        assertThat(rows.get(0).getEntityType()).isEqualTo(EntityType.USER);
        assertThat(users.findById(user.getId()).orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongPasswordIsBadCredentialsAndTenFailuresLockTheAccount() throws Exception {
        AppUser user = createUser(Role.CONSULTANT);
        String wrong = "{\"email\":\"" + user.getEmail() + "\",\"password\":\"nope\"}";
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
        }
        String right = "{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}";
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(right))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void stateChangingCallsNeedTheCsrfHeader() throws Exception {
        Client client = login(Role.CONSULTANT);
        mvc.perform(post("/api/v1/auth/logout").session(client.session())).andExpect(status().isForbidden());
        mvc.perform(client.with(post("/api/v1/auth/logout"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").session(client.session())).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesFollowTheContract() throws Exception {
        Client consultant = login(Role.CONSULTANT);
        Client manager = login(Role.MANAGER);
        Client admin = login(Role.ADMIN);

        mvc.perform(get("/api/v1/users").session(consultant.session())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/audit/access-log").session(consultant.session())).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/audit/access-log").session(manager.session())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit/access-log").session(admin.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0));
        mvc.perform(get("/api/v1/audit/revisions").param("entityType", "USER")
                .param("entityId", admin.userId().toString()).session(consultant.session()))
                .andExpect(status().isForbidden());
        String body = "{\"email\":\"x@example.test\",\"name\":\"X\",\"role\":\"CONSULTANT\","
                + "\"initialPassword\":\"A-long-password-1\"}";
        mvc.perform(manager.with(post("/api/v1/users")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminManagesUsersAndTheChangeHistoryShowsWho() throws Exception {
        Client admin = login(Role.ADMIN);
        String email = "new-" + UUID.randomUUID() + "@example.test";
        String body = "{\"email\":\"" + email + "\",\"name\":\"New Person\",\"role\":\"CONSULTANT\","
                + "\"initialPassword\":\"A-long-password-1\"}";
        String created = mvc.perform(admin.with(post("/api/v1/users")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true)).andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");
        mvc.perform(admin.with(post("/api/v1/users")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE"));
        mvc.perform(admin.with(patch("/api/v1/users/" + id)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MANAGER\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
        mvc.perform(admin.with(patch("/api/v1/users/" + admin.userId())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/audit/revisions").param("entityType", "USER").param("entityId", id)
                .session(admin.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("MOD"))
                .andExpect(jsonPath("$.content[0].user.id").value(admin.userId().toString()))
                .andExpect(jsonPath("$.content[0].changes[?(@.field=='role')].to").value("MANAGER"))
                .andExpect(jsonPath("$.content[1].type").value("ADD"));
    }

    @Test
    void passwordChangeNeedsTheCurrentPasswordAndTwelveCharacters() throws Exception {
        Client client = login(Role.CONSULTANT);
        mvc.perform(client.with(post("/api/v1/auth/password")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(client.with(post("/api/v1/auth/password")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-password-1\",\"newPassword\":\"A-brand-new-pass-1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(client.with(post("/api/v1/auth/password")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"A-brand-new-pass-1\"}"))
                .andExpect(status().isNoContent());
    }
}
