package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.Seniority;
import com.recruiting.platform.security.IdentifiedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * S5 (R65, contract 1.3.0): bulk add of 10 people to the longlist, rank reorder, outreach (longlist) status, the
 * shortlist entry rule (rating and a rationale of at least 20 characters) and a candidate pool filter. Synthetic data.
 */
@AutoConfigureMockMvc
@Import(GuardTestConfig.class)
class LonglistApiIT extends AbstractPersistenceIT {

    private record Principal(UUID userId) implements IdentifiedUser {
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private PersonRepository people;

    private UUID consultant;

    private ResultActions call(MockHttpServletRequestBuilder b, String body) throws Exception {
        b.with(authentication(new UsernamePasswordAuthenticationToken(new Principal(consultant), "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CONSULTANT"))))).with(csrf());
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(b);
    }

    private String json(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    private UUID newPerson(int n, String skill, Seniority seniority) {
        Person p = new Person();
        p.setName("Sample Candidate " + n + " " + UUID.randomUUID());
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(consultant);
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        p.setSkills(List.of(skill));
        p.setSeniority(seniority);
        return tx.execute(s -> people.save(p)).getId();
    }

    @Test
    void longlistFromBulkAddToShortlistEntry() throws Exception {
        consultant = newUser().getId();
        Account acc = new Account();
        acc.setName("Longlist Sample Corp " + UUID.randomUUID());
        acc.setSector("Retail");
        acc.setOwnerId(consultant);
        acc.setStatus(AccountStatus.ACTIVE);
        acc.setStatusChangedAt(Instant.now());
        acc.setOpenedOn(LocalDate.now());
        acc.setOpenJobCount(0);
        UUID accountId = tx.execute(s -> accounts.save(acc)).getId();
        String job = JsonPath.read(json(call(post("/api/v1/job-orders"),
                "{\"accountId\":\"" + accountId + "\",\"title\":\"Longlist sample search\"}")
                .andExpect(status().isCreated())), "$.id");

        // Pool filter data: a unique skill tag on 5 of the 10 people, 3 of them at HEAD level.
        String skill = "pool" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        List<UUID> persons = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            persons.add(newPerson(i, i < 5 ? skill : "other", i < 3 ? Seniority.HEAD : Seniority.SPECIALIST));
        }

        // Pool filter query (contract 1.3.0: skill, seniority).
        assertThat(JsonPath.<List<?>>read(json(call(get("/api/v1/people?skill=" + skill), null)
                .andExpect(status().isOk())), "$.content")).hasSize(5);
        assertThat(JsonPath.<List<?>>read(json(call(get("/api/v1/people?skill=" + skill + "&seniority=HEAD"), null)
                .andExpect(status().isOk())), "$.content")).hasSize(3);

        // Bulk add of 10 people: appended in order, ranks 1..10 on the longlist, outreach TO_CALL.
        List<String> apps = new ArrayList<>();
        for (int i = 0; i < persons.size(); i++) {
            ResultActions r = call(post("/api/v1/job-orders/" + job + "/applications"),
                    "{\"personId\":\"" + persons.get(i) + "\"}").andExpect(status().isCreated())
                    .andExpect(jsonPath("$.list").value("LONGLIST"))
                    .andExpect(jsonPath("$.rank").value(i + 1))
                    .andExpect(jsonPath("$.outreachStatus").value("TO_CALL"));
            apps.add(JsonPath.read(json(r), "$.id"));
        }
        call(get("/api/v1/job-orders/" + job + "/applications?list=LONGLIST"), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value(apps.get(0))).andExpect(jsonPath("$[9].rank").value(10));

        // Rank reorder: reverse the list; ranks stay 1..10 and the response is in the new order.
        List<String> reversed = new ArrayList<>(apps);
        Collections.reverse(reversed);
        call(put("/api/v1/job-orders/" + job + "/ranking"), "{\"list\":\"LONGLIST\",\"applicationIds\":"
                + toJson(reversed) + "}").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(apps.get(9))).andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[9].id").value(apps.get(0))).andExpect(jsonPath("$[9].rank").value(10));
        // A stale view (one id missing) is refused.
        call(put("/api/v1/job-orders/" + job + "/ranking"), "{\"list\":\"LONGLIST\",\"applicationIds\":"
                + toJson(reversed.subList(1, 10)) + "}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        // Longlist (outreach) status change.
        String app = apps.get(0);
        call(patch("/api/v1/applications/" + app), "{\"outreachStatus\":\"CALLED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.outreachStatus").value("CALLED"));
        call(patch("/api/v1/applications/" + app + "/stage"), "{\"stage\":\"SCREENED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.outreachStatus").value("SCREENED"))
                .andExpect(jsonPath("$.list").value("LONGLIST"));

        // Shortlist entry needs a rating and a rationale of at least 20 characters.
        call(patch("/api/v1/applications/" + app + "/stage"), "{\"stage\":\"PRESENTED\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRANSITION_GUARD_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field=='rationale')]").exists());
        call(patch("/api/v1/applications/" + app), "{\"rating\":4,\"rationale\":\"too short\"}")
                .andExpect(status().isOk());
        call(patch("/api/v1/applications/" + app + "/stage"), "{\"stage\":\"PRESENTED\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRANSITION_GUARD_FAILED"));
        call(patch("/api/v1/applications/" + app),
                "{\"rationale\":\"Strong fit on scope and sector; call confirmed motivation.\"}")
                .andExpect(status().isOk());
        call(patch("/api/v1/applications/" + app + "/stage"), "{\"stage\":\"PRESENTED\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.list").value("SHORTLIST")).andExpect(jsonPath("$.rank").value(1));
        // The gap on the longlist is closed: 9 left, ranks 1..9.
        call(get("/api/v1/job-orders/" + job + "/applications?list=LONGLIST"), null)
                .andExpect(jsonPath("$.length()").value(9)).andExpect(jsonPath("$[8].rank").value(9));
    }

    private static String toJson(List<String> ids) {
        return "[\"" + String.join("\",\"", ids) + "\"]";
    }
}
