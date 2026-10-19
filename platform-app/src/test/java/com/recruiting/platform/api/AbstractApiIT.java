package com.recruiting.platform.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.persistence.PostgresTestConfig;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base for API tests: the whole application on a real PostgreSQL, called through MockMvc with the real filter chain
 * (session, CSRF, roles). {@link #login} walks the same steps as the SPA: GET for the XSRF cookie, then POST login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
@org.springframework.test.context.TestPropertySource(properties = "app.security.login-rate-per-minute=10000")
public abstract class AbstractApiIT {

    public static final String PASSWORD = "Test-Password-2026";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected AppUserRepository users;

    @Autowired
    protected PasswordEncoder encoder;

    /** A logged-in browser: session cookie plus the CSRF token to echo. */
    public record Client(MockHttpSession session, String csrf, UUID userId) {

        public MockHttpServletRequestBuilder with(MockHttpServletRequestBuilder request) {
            return request.session(session).cookie(new Cookie("XSRF-TOKEN", csrf)).header("X-XSRF-TOKEN", csrf);
        }
    }

    protected AppUser createUser(Role role) {
        AppUser user = new AppUser();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test");
        user.setName(role + " Tester");
        user.setRole(role);
        user.setLanguage(Language.EN);
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setMustChangePassword(false);
        return users.saveAndFlush(user);
    }

    protected Client login(Role role) throws Exception {
        return login(createUser(role));
    }

    protected Client login(AppUser user) throws Exception {
        MvcResult first = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/auth/me")).andReturn();
        Cookie xsrf = first.getResponse().getCookie("XSRF-TOKEN");
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        return new Client(session, xsrf.getValue(), user.getId());
    }
}
