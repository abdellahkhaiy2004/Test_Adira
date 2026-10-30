package com.example.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The Config Server must actually serve the shared business parameter required by the exam. */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void servesTheTransactionCeilingToTransactionService() throws Exception {
        mvc.perform(get("/transaction-service/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("transaction-service"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bank.transaction.max-amount")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1000")));
    }

    @Test
    void servesSharedDefaultsAndPerServiceFiles() throws Exception {
        mvc.perform(get("/account-service/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("account-service"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bank.account.default-currency")));
    }

    @Test
    void unknownServiceStillAnswersWithSharedConfigurationOnly() throws Exception {
        mvc.perform(get("/does-not-exist/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("does-not-exist"));
    }
}
