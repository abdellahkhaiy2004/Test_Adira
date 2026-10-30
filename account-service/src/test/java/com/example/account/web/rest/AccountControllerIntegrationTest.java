package com.example.account.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through REST -> service -> JPA/H2. */
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String createAccount(String body) throws Exception {
        MvcResult r = mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        Matcher m = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"").matcher(r.getResponse().getContentAsString());
        if (!m.find()) {
            throw new AssertionError("no id in response");
        }
        return m.group(1);
    }

    @Test
    void createThenGet() throws Exception {
        String id = createAccount("{\"customerReference\":\"CUST-1\",\"currency\":\"EUR\",\"balance\":100.5}");

        mvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value("CUST-1"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(100.5));
    }

    @Test
    void listFiltersByCustomerReference() throws Exception {
        createAccount("{\"customerReference\":\"LIST-ME\",\"currency\":\"USD\"}");

        mvc.perform(get("/api/accounts").param("customerReference", "LIST-ME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].balance").value(0.0));
    }

    @Test
    void blockAndReactivate() throws Exception {
        String id = createAccount("{\"customerReference\":\"C\",\"currency\":\"EUR\"}");

        mvc.perform(patch("/api/accounts/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(patch("/api/accounts/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mvc.perform(get("/api/accounts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidInputIs400() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerReference\":\"\",\"currency\":\"EUR\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerReference\":\"C\",\"currency\":\"EUR\",\"balance\":-5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerReference\":\"C\",\"currency\":\"EUR\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/accounts/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
