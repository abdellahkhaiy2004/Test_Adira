package com.example.transaction.mcp;

import com.example.transaction.FakeAccountServer;
import com.example.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The MCP connector must expose the banking tools and route them through the same service layer. */
@SpringBootTest
class TransactionToolsIntegrationTest {

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
    ToolCallbackProvider provider;
    @Autowired
    TransactionRepository transactions;

    private ToolCallback tool(String name) {
        return Arrays.stream(provider.getToolCallbacks())
                .filter(c -> name.equals(c.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tool not registered: " + name));
    }

    /** Returns the tool result, or the failure message when the tool rejects the call. */
    private String call(String name, String jsonArguments) {
        try {
            return tool(name).call(jsonArguments);
        } catch (RuntimeException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Test
    void bankingToolsAreRegistered() {
        assertThat(Arrays.stream(provider.getToolCallbacks()).map(c -> c.getToolDefinition().name()))
                .contains("list_transactions", "get_transaction", "create_transaction", "transfer_money");
    }

    @Test
    void createTransactionToolDebitsThroughTheServiceLayer() {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");

        String result = call("create_transaction", "{\"accountId\":\"" + acc + "\",\"amount\":\"250.00\","
                + "\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"MA\"}");

        assertThat(result).contains("GROCERY");
        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(250.0);
        assertThat(transactions.findByAccountIdOrderByCreatedAtDesc(acc)).hasSize(1);
    }

    @Test
    void transferToolMovesMoneyBetweenAccounts() {
        UUID from = ACCOUNTS.addAccount("ACTIVE", "EUR", "500");
        UUID to = ACCOUNTS.addAccount("ACTIVE", "EUR", "100");

        String result = call("transfer_money", "{\"sourceAccountId\":\"" + from + "\",\"destinationAccountId\":\""
                + to + "\",\"amount\":\"200\",\"currency\":\"EUR\",\"country\":\"MA\"}");

        assertThat(result).contains(from.toString()).contains(to.toString());
        assertThat(ACCOUNTS.balanceOf(from)).isEqualTo(300.0);
        assertThat(ACCOUNTS.balanceOf(to)).isEqualTo(300.0);
    }

    /** The assistant is bound by the configured ceiling and the account rules, exactly like REST. */
    @Test
    void businessRulesStillApplyThroughMcp() {
        UUID acc = ACCOUNTS.addAccount("ACTIVE", "EUR", "5000");
        UUID blocked = ACCOUNTS.addAccount("BLOCKED", "EUR", "5000");

        assertThat(call("create_transaction", "{\"accountId\":\"" + acc + "\",\"amount\":\"1000.01\","
                + "\"currency\":\"EUR\",\"merchantCategory\":\"X\",\"country\":\"MA\"}"))
                .containsIgnoringCase("maximum");

        assertThat(call("create_transaction", "{\"accountId\":\"" + blocked + "\",\"amount\":\"10\","
                + "\"currency\":\"EUR\",\"merchantCategory\":\"X\",\"country\":\"MA\"}"))
                .containsIgnoringCase("ACTIVE");

        assertThat(call("create_transaction", "{\"accountId\":\"" + acc + "\",\"amount\":\"10\","
                + "\"currency\":\"USD\",\"merchantCategory\":\"X\",\"country\":\"MA\"}"))
                .containsIgnoringCase("currency");

        assertThat(ACCOUNTS.balanceOf(acc)).isEqualTo(5000.0);
        assertThat(ACCOUNTS.balanceOf(blocked)).isEqualTo(5000.0);
    }
}
