package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.recruiting.platform.security.IdentifiedUser;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** S4 quick log: outcome, direction, duration, attendees and e-mail idempotency (contract 1.3.0, R70). */
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(GuardTestConfig.class)
class ActivityQuickLogApiIT extends AbstractPersistenceIT {

    private record Principal(UUID userId) implements IdentifiedUser {
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private PersonRepository people;

    private UUID user;
    private UUID accountId;
    private UUID main;
    private UUID guest;
    private UUID guest2;

    @BeforeEach
    void setUp() {
        user = newUser().getId();
        Account a = new Account();
        a.setName("Quick Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(user);
        a.setStatus(AccountStatus.ACTIVE);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        accountId = tx.execute(s -> accounts.save(a)).getId();
        main = newPerson(RetentionStatus.ACTIVE);
        guest = newPerson(RetentionStatus.ACTIVE);
        guest2 = newPerson(RetentionStatus.ACTIVE);
    }

    private UUID newPerson(RetentionStatus rs) {
        Person p = new Person();
        p.setName("Candidate " + UUID.randomUUID());
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(user);
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(rs);
        return tx.execute(s -> people.save(p)).getId();
    }

    private ResultActions call(MockHttpServletRequestBuilder b, String body) throws Exception {
        b.with(authentication(new UsernamePasswordAuthenticationToken(new Principal(user), "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_CONSULTANT"))))).with(csrf());
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(b);
    }

    private String id(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private Instant lastContact(UUID personId) {
        return tx.execute(s -> people.findById(personId).orElseThrow().getLastContactAt());
    }

    private String body(String type, String extra) {
        return "{\"type\":\"" + type + "\",\"note\":\"Called\",\"accountId\":\"" + accountId + "\",\"personId\":\""
                + main + "\"" + extra + "}";
    }

    @Test
    void quickLogStoresAndReturnsTheNewFields() throws Exception {
        call(post("/api/v1/activities"), body("CALL", ",\"outcome\":\"CONNECTED\",\"direction\":\"OUTBOUND\","
                + "\"durationMin\":12,\"attendeePersonIds\":[\"" + guest + "\"]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("CONNECTED"))
                .andExpect(jsonPath("$.direction").value("OUTBOUND"))
                .andExpect(jsonPath("$.durationMin").value(12))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.attendees.length()").value(1))
                .andExpect(jsonPath("$.attendees[0].id").value(guest.toString()));
    }

    @Test
    void directionIsRejectedOnANote() throws Exception {
        call(post("/api/v1/activities"), body("NOTE", ",\"direction\":\"INBOUND\"")).andExpect(status().isBadRequest());
    }

    @Test
    void durationOutOfRangeIsRejected() throws Exception {
        call(post("/api/v1/activities"), body("CALL", ",\"durationMin\":1441")).andExpect(status().isBadRequest());
    }

    @Test
    void attendeeRulesAreEnforced() throws Exception {
        call(post("/api/v1/activities"), body("CALL", ",\"attendeePersonIds\":[\"" + main + "\"]"))
                .andExpect(status().isBadRequest());
        UUID gone = newPerson(RetentionStatus.ANONYMISED);
        call(post("/api/v1/activities"), body("CALL", ",\"attendeePersonIds\":[\"" + gone + "\"]"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/activities"), body("CALL", ",\"attendeePersonIds\":[\"" + UUID.randomUUID() + "\"]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void externalMessageIdNeedsEmailAndDuplicateGives409() throws Exception {
        String mid = "<" + UUID.randomUUID() + "@mail.test>";
        call(post("/api/v1/activities"), body("EMAIL", ",\"externalMessageId\":\"" + mid + "\""))
                .andExpect(status().isBadRequest());
        String first = id(call(post("/api/v1/activities"),
                body("EMAIL", ",\"source\":\"EMAIL\",\"externalMessageId\":\"" + mid + "\""))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.externalMessageId").value(mid)));
        call(post("/api/v1/activities"), body("EMAIL", ",\"source\":\"EMAIL\",\"externalMessageId\":\"" + mid + "\""))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE"))
                .andExpect(jsonPath("$.existingId").value(first));
    }

    @Test
    void updateReplacesAttendeesAndCorrectsOutcome() throws Exception {
        String a = id(call(post("/api/v1/activities"),
                body("MEETING", ",\"attendeePersonIds\":[\"" + guest + "\"]")).andExpect(status().isCreated()));
        call(patch("/api/v1/activities/" + a), "{\"attendeePersonIds\":[\"" + guest2 + "\"],\"outcome\":\"INTERESTED\","
                + "\"durationMin\":30,\"direction\":\"INBOUND\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees.length()").value(1))
                .andExpect(jsonPath("$.attendees[0].id").value(guest2.toString()))
                .andExpect(jsonPath("$.outcome").value("INTERESTED"))
                .andExpect(jsonPath("$.durationMin").value(30));
        call(patch("/api/v1/activities/" + a), "{\"outcome\":null,\"attendeePersonIds\":[]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").isEmpty())
                .andExpect(jsonPath("$.attendees.length()").value(0));
        call(patch("/api/v1/activities/" + a), "{\"source\":\"EMAIL\"}").andExpect(status().isBadRequest());
    }

    @Test
    void personFilterMatchesAttendeesToo() throws Exception {
        call(post("/api/v1/activities"), body("CALL", ",\"outcome\":\"NO_ANSWER\",\"attendeePersonIds\":[\"" + guest + "\"]"))
                .andExpect(status().isCreated());
        call(get("/api/v1/activities").param("personId", guest.toString()), null)
                .andExpect(jsonPath("$.content.length()").value(1));
        call(get("/api/v1/activities").param("personId", main.toString()).param("outcome", "CONNECTED"), null)
                .andExpect(jsonPath("$.content.length()").value(0));
        call(get("/api/v1/activities").param("personId", main.toString()).param("outcome", "NO_ANSWER"), null)
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void lastContactIsBumpedForPersonAndEveryAttendee() throws Exception {
        call(post("/api/v1/activities"), body("CALL", ",\"attendeePersonIds\":[\"" + guest + "\",\"" + guest2 + "\"]"))
                .andExpect(status().isCreated());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(lastContact(main)).isNotNull();
            assertThat(lastContact(guest)).isNotNull();
            assertThat(lastContact(guest2)).isNotNull();
        });
    }

    @Test
    void aNoteDoesNotCountAsContactForAnyone() throws Exception {
        UUID other = newPerson(RetentionStatus.ACTIVE);
        UUID otherGuest = newPerson(RetentionStatus.ACTIVE);
        call(post("/api/v1/activities"), "{\"type\":\"NOTE\",\"note\":\"fyi\",\"personId\":\"" + other
                + "\",\"attendeePersonIds\":[\"" + otherGuest + "\"]}").andExpect(status().isCreated());
        // a later call proves the first event was processed before we assert the negative
        call(post("/api/v1/activities"), body("CALL", "")).andExpect(status().isCreated());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(lastContact(main)).isNotNull());
        assertThat(lastContact(other)).isNull();
        assertThat(lastContact(otherGuest)).isNull();
    }
}
