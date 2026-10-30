package com.example.transaction.web.rest;

import com.example.transaction.FakeAccountServer;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.TransferRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST -> services -> real Feign/Resilience4J -> fake account-service over HTTP; checks business consistency. */
@SpringBootTest(properties = "bank.resilience.minimum-calls=1000")
@AutoConfigureMockMvc
class TransactionFlowIntegrationTest {

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
    @Autowired
    TransferRepository transfers;

    @BeforeEach
    void reset() {
        ACCOUNTS.reset();
        transactions.deleteAll();
        transfers.deleteAll();
    }

    private ResultActions createTx(UUID account, String amount, String currency) throws Exception {
        return mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + account + "\",\"amount\":" + amount + ",\"currency\":\"" + currency
                        + "\",\"merchantCategory\":\"GROCERY\",\"country\":\"MA\"}"));
    }

    private ResultActions transfer(UUID from, UUID to, String amount, String currency) throws Exception {
        return mvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceAccountId\":\"" + from + "\",\"destinationAccountId\":\"" + to
                        + "\",\"amount\":" + amount + ",\"currency\":\"" + currency + "\",\"country\":\"MA\"}"));
    }

    // ---- transactions -------------------------------------------------------------------------------

    @Test
    void createTransactionPersistsAndDebitsBalance() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        createTx(acc, "250.00", "EUR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(acc.toString()))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.merchantCategory").value("GROCERY"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(250.0);
        assertThat(transactions.findByAccountIdOrderByCreatedAtDesc(acc)).hasSize(1);
    }

    @Test
    void amountEqualToConfiguredMaximumIsAcceptedAndAboveIsRejected() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "5000");

        createTx(acc, "1000.00", "EUR").andExpect(status().isCreated());
        createTx(acc, "1000.01", "EUR").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AMOUNT_EXCEEDS_MAX"));

        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(4000.0);
        assertThat(transactions.count()).isEqualTo(1);
    }

    @Test
    void invalidAmountsAreRejectedWithoutCallingAccountService() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        int before = ACCOUNTS.requestCount();

        createTx(acc, "0", "EUR").andExpect(status().isBadRequest());
        createTx(acc, "-5", "EUR").andExpect(status().isBadRequest());
        createTx(acc, "1.005", "EUR").andExpect(status().isBadRequest());
        createTx(acc, "null", "EUR").andExpect(status().isBadRequest());
        createTx(acc, "10", "euro").andExpect(status().isBadRequest());

        assertThat(ACCOUNTS.requestCount()).isEqualTo(before);
        assertThat(transactions.count()).isZero();
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountIs404() throws Exception {
        createTx(UUID.randomUUID(), "10", "EUR")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        assertThat(transactions.count()).isZero();
    }

    @Test
    void blockedAccountIsRejected() throws Exception {
        UUID acc = ACCOUNTS.addAccount("BLOCKED", "EUR", "500");

        createTx(acc, "10", "EUR").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        assertThat(transactions.count()).isZero();
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(500.0);
    }

    @Test
    void currencyMismatchIsRejected() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        createTx(acc, "10", "USD").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
        assertThat(transactions.count()).isZero();
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(500.0);
    }

    @Test
    void insufficientFundsIsRejected() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "50");

        createTx(acc, "50.01", "EUR").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
        assertThat(transactions.count()).isZero();
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(50.0);
    }

    @Test
    void failedDebitRollsBackTheRecordedTransaction() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        ACCOUNTS.failDebit(true);

        createTx(acc, "100", "EUR").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ACCOUNT_SERVICE_UNAVAILABLE"));

        assertThat(transactions.count()).isZero();
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(500.0);
    }

    @Test
    void accountServiceFailingOnConsultationIs503AndNothingIsRecorded() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        ACCOUNTS.failEverything(true);

        createTx(acc, "100", "EUR").andExpect(status().isServiceUnavailable());
        assertThat(transactions.count()).isZero();
    }

    @Test
    void historyIsPerAccountAndNewestFirst() throws Exception {
        UUID a = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID b = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        createTx(a, "10", "EUR").andExpect(status().isCreated());
        createTx(a, "20", "EUR").andExpect(status().isCreated());
        createTx(b, "30", "EUR").andExpect(status().isCreated());

        mvc.perform(get("/api/transactions").param("accountId", a.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(20.0))
                .andExpect(jsonPath("$[1].amount").value(10.0));
        mvc.perform(get("/api/transactions")).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getTransactionById() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        createTx(acc, "10", "EUR").andExpect(status().isCreated());
        UUID id = transactions.findAll().get(0).getId();

        mvc.perform(get("/api/transactions/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/transactions/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/transactions/nope")).andExpect(status().isBadRequest());
    }

    // ---- transfers ----------------------------------------------------------------------------------

    @Test
    void transferMovesMoneyAndRecordsBothSides() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");

        transfer(from, to, "200", "EUR").andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceAccountId").value(from.toString()))
                .andExpect(jsonPath("$.destinationAccountId").value(to.toString()));

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(300.0);
        assertThat(ACCOUNTS.balanceOf(to)).isEqualTo(300.0);
        assertThat(transfers.count()).isEqualTo(1);
        assertThat(transactions.findByAccountIdOrderByCreatedAtDesc(from)).singleElement()
                .extracting("merchantCategory").isEqualTo("TRANSFER_OUT");
        assertThat(transactions.findByAccountIdOrderByCreatedAtDesc(to)).singleElement()
                .extracting("merchantCategory").isEqualTo("TRANSFER_IN");
    }

    @Test
    void transferToSameAccountIsRejected() throws Exception {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        transfer(acc, acc, "10", "EUR").andExpect(status().isBadRequest());
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(500.0);
    }

    @Test
    void transferRespectsAmountCeilingAndFunds() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "5000");
        UUID poor = ACCOUNTS.addAccount("ACTIVE", "EUR", "5");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "0");

        transfer(from, to, "1000.01", "EUR").andExpect(status().isBadRequest());
        transfer(poor, to, "10", "EUR").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(5000.0);
        assertThat(ACCOUNTS.balanceOf(to)).isZero();
        assertThat(transfers.count()).isZero();
    }

    @Test
    void transferRejectsBlockedOrMismatchedAccountsAndUnknownAccounts() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID blocked = ACCOUNTS.addAccount("BLOCKED", "EUR", "0");
        UUID usd = ACCOUNTS.addAccount("ACTIVE", "USD", "0");

        transfer(from, blocked, "10", "EUR").andExpect(status().isConflict());
        transfer(from, usd, "10", "EUR").andExpect(status().isUnprocessableContent());
        transfer(from, UUID.randomUUID(), "10", "EUR").andExpect(status().isNotFound());

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(500.0);
        assertThat(transfers.count()).isZero();
        assertThat(transactions.count()).isZero();
    }

    @Test
    void failedCreditRefundsSourceAndRecordsNothing() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");
        // Destination refuses the credit after the pre-check passed (state changed in between).
        ACCOUNTS.rejectCreditsTo(to);

        transfer(from, to, "200", "EUR").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REJECTED_BY_ACCOUNT_SERVICE"));

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(500.0);
        assertThat(ACCOUNTS.balanceOf(to)).isEqualTo(100.0);
        assertThat(transfers.count()).isZero();
        assertThat(transactions.count()).isZero();
    }

    @Test
    void failedDebitDuringTransferChangesNothing() throws Exception {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");
        ACCOUNTS.failDebit(true);

        transfer(from, to, "200", "EUR").andExpect(status().isServiceUnavailable());

        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(500.0);
        assertThat(ACCOUNTS.balanceOf(to)).isEqualTo(100.0);
        assertThat(transfers.count()).isZero();
        assertThat(transactions.count()).isZero();
    }
}
