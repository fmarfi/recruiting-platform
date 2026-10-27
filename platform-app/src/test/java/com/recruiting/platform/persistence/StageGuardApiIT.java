package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.recruiting.platform.contracts.ExposureChannel;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import com.recruiting.platform.contracts.ShareBlockReason;
import com.recruiting.platform.pipeline.domain.ApplicationRepository;
import com.recruiting.platform.pipeline.domain.CandidateExposureRepository;
import com.recruiting.platform.security.IdentifiedUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Slices 2 and 3 of Wave 2b: the sharing guard (CONSENT_REQUIRED on every move into a client-facing stage), the
 * off-limits guard with the manager override, the exposure written once, the exposure endpoint and the warnings.
 * ShareGuard and OffLimitsApi are test doubles until privacy and accounts implementations are merged.
 */
@AutoConfigureMockMvc
@Import(GuardTestConfig.class)
class StageGuardApiIT extends AbstractPersistenceIT {

    private record Principal(UUID userId) implements IdentifiedUser {
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private PersonRepository people;
    @Autowired
    private ApplicationRepository applications;
    @Autowired
    private CandidateExposureRepository exposures;
    @Autowired
    private GuardTestConfig.StubShareGuard shareGuard;
    @Autowired
    private GuardTestConfig.StubOffLimits offLimits;

    private UUID consultant;
    private UUID manager;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        consultant = newUser().getId();
        manager = newUser().getId();
        accountId = newAccount();
    }

    private UUID newAccount() {
        Account a = new Account();
        a.setName("Guard Corp " + UUID.randomUUID());
        a.setSector("Retail");
        a.setOwnerId(consultant);
        a.setStatus(AccountStatus.ACTIVE);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        return tx.execute(s -> accounts.save(a)).getId();
    }

    private UUID newPerson() {
        Person p = new Person();
        p.setName("Candidate " + UUID.randomUUID());
        p.setType(PersonType.CANDIDATE);
        p.setOwnerId(consultant);
        p.setConsentStatus(ConsentStatus.NOT_SENT);
        p.setRetentionStatus(RetentionStatus.ACTIVE);
        return tx.execute(s -> people.save(p)).getId();
    }

    private ResultActions call(MockHttpServletRequestBuilder b, UUID who, String role, String body) throws Exception {
        b.with(authentication(new UsernamePasswordAuthenticationToken(new Principal(who), "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))))).with(csrf());
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(b);
    }

    private String id(ResultActions r) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String newJob(UUID account) throws Exception {
        return id(call(post("/api/v1/job-orders"), consultant, "CONSULTANT",
                "{\"accountId\":\"" + account + "\",\"title\":\"Guard job " + UUID.randomUUID() + "\"}")
                .andExpect(status().isCreated()));
    }

    private String add(String job, UUID person) throws Exception {
        return id(call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + person + "\"}").andExpect(status().isCreated()));
    }

    private ResultActions move(String app, String stage, UUID who, String role, String extra) throws Exception {
        return call(patch("/api/v1/applications/" + app + "/stage"), who, role,
                "{\"stage\":\"" + stage + "\"" + (extra == null ? "" : "," + extra) + "}");
    }

    @Test
    void everyMoveIntoAClientFacingStageNeedsConsent() throws Exception {
        UUID person = newPerson();
        String app = add(newJob(accountId), person);
        shareGuard.block(person, ShareBlockReason.CONSENT_PENDING);
        for (String stage : List.of("PRESENTED", "CLIENT_INTERVIEW", "OFFER")) {
            move(app, stage, consultant, "CONSULTANT", null).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"))
                    .andExpect(jsonPath("$.blockingItems[0].type").value("PERSON"))
                    .andExpect(jsonPath("$.blockingItems[0].id").value(person.toString()))
                    .andExpect(jsonPath("$.blockingItems[0].reasonCode").value("CONSENT_PENDING"));
        }
        // a manager cannot override consent, even with an off-limits reason
        move(app, "PRESENTED", manager, "MANAGER", "\"offLimitsOverrideReason\":\"Agreed with the client\"")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
        assertThat(applications.findById(UUID.fromString(app)).orElseThrow().getStage().name()).isEqualTo("SOURCED");
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(person)).isEmpty();
        // moves that do not share stay possible, and consent given later lets the move through
        move(app, "SCREENED", consultant, "CONSULTANT", null).andExpect(status().isOk());
        shareGuard.unblock(person);
        move(app, "CLIENT_INTERVIEW", consultant, "CONSULTANT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("CLIENT_INTERVIEW"));
    }

    @Test
    void consentIsCheckedAgainOnLaterClientFacingMoves() throws Exception {
        UUID person = newPerson();
        String app = add(newJob(accountId), person);
        move(app, "PRESENTED", consultant, "CONSULTANT", null).andExpect(status().isOk());
        shareGuard.block(person, ShareBlockReason.CONSENT_WITHDRAWN);
        move(app, "CLIENT_INTERVIEW", consultant, "CONSULTANT", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockingItems[0].reasonCode").value("CONSENT_WITHDRAWN"));
        // rejecting is not sharing
        move(app, "REJECTED", consultant, "CONSULTANT", "\"reason\":\"Withdrew from the process\"")
                .andExpect(status().isOk());
    }

    @Test
    void exposureIsWrittenOnceOnTheFirstClientFacingMove() throws Exception {
        UUID person = newPerson();
        String app = add(newJob(accountId), person);
        move(app, "OFFER", consultant, "CONSULTANT", null).andExpect(status().isOk()); // skips PRESENTED
        var written = exposures.findByPersonIdOrderByPresentedAtDesc(person);
        assertThat(written).hasSize(1);
        assertThat(written.get(0).getAccountId()).isEqualTo(accountId);
        assertThat(written.get(0).getApplicationId().toString()).isEqualTo(app);
        assertThat(written.get(0).getChannel()).isEqualTo(ExposureChannel.PIPELINE);
        assertThat(written.get(0).getPresentedBy()).isEqualTo(consultant);
        move(app, "REJECTED", consultant, "CONSULTANT", "\"reason\":\"Offer declined\"").andExpect(status().isOk());
        move(app, "SCREENED", consultant, "CONSULTANT", null).andExpect(status().isOk());
        move(app, "PRESENTED", consultant, "CONSULTANT", null).andExpect(status().isOk());
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(person)).hasSize(1);
    }

    @Test
    void offLimitsBlocksTheFirstClientFacingMoveUnlessAManagerOverrides() throws Exception {
        UUID person = newPerson();
        String app = add(newJob(accountId), person);
        offLimits.put(person, "Anadolu Lojistik");
        // consent is checked before off-limits
        shareGuard.block(person, ShareBlockReason.CONSENT_NOT_REQUESTED);
        move(app, "PRESENTED", consultant, "CONSULTANT", null).andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
        shareGuard.unblock(person);
        move(app, "PRESENTED", consultant, "CONSULTANT", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OFF_LIMITS"))
                .andExpect(jsonPath("$.blockingItems[0].type").value("OFF_LIMITS_RULE"))
                .andExpect(jsonPath("$.blockingItems[0].reasonCode").value("CLIENT_AGREEMENT"));
        // a consultant who sends an override gets 403, a too short one 400 for the manager
        move(app, "PRESENTED", consultant, "CONSULTANT", "\"offLimitsOverrideReason\":\"Partner agreed\"")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        move(app, "PRESENTED", manager, "MANAGER", "\"offLimitsOverrideReason\":\"ab\"")
                .andExpect(status().isBadRequest());
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(person)).isEmpty();
        move(app, "PRESENTED", manager, "MANAGER", "\"offLimitsOverrideReason\":\"  Partner agreed in writing \"")
                .andExpect(status().isOk()).andExpect(jsonPath("$.offLimitsOverrideReason").value("Partner agreed in writing"));
        assertThat(applications.findById(UUID.fromString(app)).orElseThrow().getOffLimitsOverrideReason())
                .isEqualTo("Partner agreed in writing");
        // the stored override covers later moves by a consultant; the exposure exists once
        move(app, "CLIENT_INTERVIEW", consultant, "CONSULTANT", null).andExpect(status().isOk());
        assertThat(exposures.findByPersonIdOrderByPresentedAtDesc(person)).hasSize(1);
    }

    @Test
    void offLimitsOnAddNeedsAManagerOverride() throws Exception {
        UUID person = newPerson();
        String job = newJob(accountId);
        offLimits.put(person, "Anadolu Lojistik");
        call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + person + "\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OFF_LIMITS"));
        call(post("/api/v1/job-orders/" + job + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + person + "\",\"offLimitsOverrideReason\":\"Partner agreed\"}")
                .andExpect(status().isForbidden());
        call(post("/api/v1/job-orders/" + job + "/applications"), manager, "MANAGER",
                "{\"personId\":\"" + person + "\",\"offLimitsOverrideReason\":\"Partner agreed\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.offLimitsOverrideReason").value("Partner agreed"));
    }

    @Test
    void exposureEndpointListsPresentationsWithOutcomeAndFiltersByAccount() throws Exception {
        UUID person = newPerson();
        UUID other = newAccount();
        String app1 = add(newJob(accountId), person);
        String app2 = add(newJob(other), person);
        move(app1, "PRESENTED", consultant, "CONSULTANT", null).andExpect(status().isOk());
        move(app2, "OFFER", consultant, "CONSULTANT", null).andExpect(status().isOk());
        move(app1, "REJECTED", consultant, "CONSULTANT", "\"reason\":\"Not a fit\"").andExpect(status().isOk());
        call(get("/api/v1/people/" + person + "/exposure"), consultant, "CONSULTANT", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].applicationId").value(app2)).andExpect(jsonPath("$[0].stage").value("OFFER"))
                .andExpect(jsonPath("$[0].channel").value("PIPELINE"))
                .andExpect(jsonPath("$[1].stage").value("REJECTED"))
                .andExpect(jsonPath("$[1].account.id").value(accountId.toString()))
                .andExpect(jsonPath("$[1].presentedBy.id").value(consultant.toString()));
        call(get("/api/v1/people/" + person + "/exposure").param("accountId", other.toString()), consultant,
                "CONSULTANT", null).andExpect(jsonPath("$.length()").value(1));
        call(get("/api/v1/people/" + UUID.randomUUID() + "/exposure"), consultant, "CONSULTANT", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void representingToTheSameAccountAndActiveElsewhereGiveWarnings() throws Exception {
        UUID person = newPerson();
        String job1 = newJob(accountId);
        String app1 = add(newJob(accountId), person);
        move(app1, "PRESENTED", consultant, "CONSULTANT", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").doesNotExist());
        // adding to a second job order of the same account: both warnings
        call(post("/api/v1/job-orders/" + job1 + "/applications"), consultant, "CONSULTANT",
                "{\"personId\":\"" + person + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings.length()").value(2))
                .andExpect(jsonPath("$.warnings[?(@.code=='PREVIOUSLY_PRESENTED_TO_ACCOUNT')].exposures[0].applicationId")
                        .value(app1))
                .andExpect(jsonPath("$.warnings[?(@.code=='ACTIVE_IN_OTHER_PROCESS')].otherProcesses[0].applicationId")
                        .value(app1));
        // lists carry no warnings
        call(get("/api/v1/job-orders/" + job1 + "/applications"), consultant, "CONSULTANT", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].warnings").doesNotExist());
    }
}
