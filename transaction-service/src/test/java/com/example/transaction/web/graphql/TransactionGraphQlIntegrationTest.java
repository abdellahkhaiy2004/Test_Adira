package com.example.transaction.web.graphql;

import com.example.transaction.FakeAccountServer;
import com.example.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GraphQL connector reaching the same TransactionService / TransferService as REST. */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionGraphQlIntegrationTest {

    private static final FakeAccountServer ACCOUNTS = new FakeAccountServer();

    @DynamicPropertySource
    static void accountServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("bank.account-service.url", ACCOUNTS::url);
    }

    @AfterAll
    static void stop() {
        ACCOUNTS.close();
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    TransactionRepository transactions;

    private ResultActions graphql(String document) throws Exception {
        return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"" + document + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createTransactionMutationDebitsTheAccount() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        graphql("mutation { createTransaction(accountId: \\\"" + acc + "\\\", amount: \\\"250.00\\\", "
                + "currency: \\\"EUR\\\", merchantCategory: \\\"GROCERY\\\", country: \\\"MA\\\") "
                + "{ id amount currency merchantCategory } }")
                .andExpect(jsonPath("$.data.createTransaction.amount").value("250.00"))
                .andExpect(jsonPath("$.data.createTransaction.merchantCategory").value("GROCERY"));

        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(250.0);
    }

    @Test
    void transferMutationMovesMoney() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");

        graphql("mutation { transfer(sourceAccountId: \\\"" + from + "\\\", destinationAccountId: \\\"" + to
                + "\\\", amount: \\\"200\\\", currency: \\\"EUR\\\", country: \\\"MA\\\") { id amount } }")
                .andExpect(jsonPath("$.data.transfer.amount").value("200"));

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(300.0);
        assertThat(ACCOUNTS.balanceOf(to)).isEqualTo(300.0);
    }

    @Test
    void transactionsQueryReturnsHistoryForAnAccount() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        graphql("mutation { createTransaction(accountId: \\\"" + acc + "\\\", amount: \\\"10\\\", "
                + "currency: \\\"EUR\\\", merchantCategory: \\\"A\\\", country: \\\"MA\\\") { id } }");

        graphql("{ transactions(accountId: \\\"" + acc + "\\\") { amount merchantCategory } }")
                .andExpect(jsonPath("$.data.transactions.length()").value(1))
                .andExpect(jsonPath("$.data.transactions[0].merchantCategory").value("A"));
    }

    @Test
    void unknownTransactionIsNullNotAnError() throws Exception {
        graphql("{ transaction(id: \\\"" + UUID.randomUUID() + "\\\") { id } }")
                .andExpect(jsonPath("$.data.transaction").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    /** The GraphQL connector must surface the same rules and codes as REST. */
    @Test
    void businessRulesApplyAndCarryTheSameCodes() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "5000");

        graphql("mutation { createTransaction(accountId: \\\"" + acc + "\\\", amount: \\\"1000.01\\\", "
                + "currency: \\\"EUR\\\", merchantCategory: \\\"X\\\", country: \\\"MA\\\") { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("AMOUNT_EXCEEDS_MAX"));

        graphql("mutation { createTransaction(accountId: \\\"" + acc + "\\\", amount: \\\"10\\\", "
                + "currency: \\\"USD\\\", merchantCategory: \\\"X\\\", country: \\\"MA\\\") { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CURRENCY_MISMATCH"));

        assertThat(transactions.count()).isZero();
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(5000.0);
    }
}
