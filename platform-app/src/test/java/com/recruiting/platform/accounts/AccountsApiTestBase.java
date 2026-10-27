package com.recruiting.platform.accounts;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruiting.platform.api.AbstractApiIT;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Small helpers shared by the Wave 2b accounts API tests. */
abstract class AccountsApiTestBase extends AbstractApiIT {

    @Autowired
    protected JdbcTemplate jdbc;

    protected final JsonMapper json = JsonMapper.builder().build();

    protected static String unique(String p) {
        return p + UUID.randomUUID().toString().substring(0, 8);
    }

    protected JsonNode send(Client c, AbstractMockHttpServletRequestBuilder<?> b, int expected) throws Exception {
        String body = mvc.perform(b.session(c.session()).cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", c.csrf())).header("X-XSRF-TOKEN", c.csrf())).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return body.isBlank() ? null : json.readTree(body);
    }

    protected JsonNode get(Client c, String url, int expected) throws Exception {
        return send(c, MockMvcRequestBuilders.get(url), expected);
    }

    protected JsonNode post(Client c, String url, String body, int expected) throws Exception {
        return send(c, MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(body),
                expected);
    }

    protected JsonNode patch(Client c, String url, String body, int expected) throws Exception {
        return send(c, MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON).content(body),
                expected);
    }

    protected JsonNode delete(Client c, String url, int expected) throws Exception {
        return send(c, MockMvcRequestBuilders.delete(url), expected);
    }

    /** Creates a person as {@code c} and returns the response; {@code extra} is raw JSON members (with leading comma). */
    protected JsonNode createPerson(Client c, String name, String extra) throws Exception {
        return post(c, "/api/v1/people", "{\"name\":\"" + name + "\",\"type\":\"CANDIDATE\"" + extra + "}", 201);
    }
}
