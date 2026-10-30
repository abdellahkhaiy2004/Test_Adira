package com.example.account.web.graphql;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GraphQL connector reaching the same AccountService as REST. Posting raw GraphQL over MockMvc keeps this
 * test free of any extra test dependency.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountGraphQlIntegrationTest {

    @Autowired
    MockMvc mvc;

    /** Sends a GraphQL document; {@code "} must be escaped by the caller. */
    private MvcResult graphql(String document) throws Exception {
        return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"" + document + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String createAccount(String reference, String currency, String balance) throws Exception {
        MvcResult result = graphql("mutation { createAccount(customerReference: \\\"" + reference
                + "\\\", currency: \\\"" + currency + "\\\", balance: " + balance + ") { id status balance } }");
        Matcher m = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"").matcher(result.getResponse().getContentAsString());
        if (!m.find()) {
            throw new AssertionError("no id in response: " + result.getResponse().getContentAsString());
        }
        return m.group(1);
    }

    @Test
    void createQueryAndMutateThroughGraphQl() throws Exception {
        String id = createAccount("GQL-1", "EUR", "100.0");

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ account(id: \\\"" + id + "\\\") { customerReference currency "
                                + "status balance } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account.customerReference").value("GQL-1"))
                .andExpect(jsonPath("$.data.account.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.account.balance").value(100.0));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { debitAccount(id: \\\"" + id
                                + "\\\", amount: \\\"40.50\\\") { balance } }\"}"))
                .andExpect(jsonPath("$.data.debitAccount.balance").value(59.5));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { updateAccountStatus(id: \\\"" + id
                                + "\\\", status: BLOCKED) { status } }\"}"))
                .andExpect(jsonPath("$.data.updateAccountStatus.status").value("BLOCKED"));
    }

    @Test
    void accountsQueryFiltersByCustomerReference() throws Exception {
        createAccount("GQL-FILTER", "USD", "0.0");

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ accounts(customerReference: \\\"GQL-FILTER\\\") "
                                + "{ customerReference currency } }\"}"))
                .andExpect(jsonPath("$.data.accounts.length()").value(1))
                .andExpect(jsonPath("$.data.accounts[0].currency").value("USD"));
    }

    @Test
    void unknownAccountReturnsNullNotAnError() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ account(id: \\\"" + UUID.randomUUID() + "\\\") { id } }\"}"))
                .andExpect(jsonPath("$.data.account").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    /** The GraphQL connector must surface the same rules as REST, not a weaker set. */
    @Test
    void businessRulesApplyAndCarryTheSameCodes() throws Exception {
        String id = createAccount("GQL-RULES", "EUR", "10.0");

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { debitAccount(id: \\\"" + id
                                + "\\\", amount: \\\"10.01\\\") { balance } }\"}"))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("INSUFFICIENT_FUNDS"));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { createAccount(customerReference: \\\"\\\", "
                                + "currency: \\\"EUR\\\") { id } }\"}"))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("INVALID_REQUEST"));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"mutation { creditAccount(id: \\\"" + UUID.randomUUID()
                                + "\\\", amount: \\\"1\\\") { balance } }\"}"))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("ACCOUNT_NOT_FOUND"));
    }
}
