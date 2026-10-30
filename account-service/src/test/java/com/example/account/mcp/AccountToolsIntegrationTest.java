package com.example.account.mcp;

import com.example.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The MCP connector must expose the banking tools and route them through the same AccountService. */
@SpringBootTest
class AccountToolsIntegrationTest {

    @Autowired
    ToolCallbackProvider provider;
    @Autowired
    AccountRepository repository;

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
    void bankingToolsAreRegisteredWithDescriptions() {
        assertThat(Arrays.stream(provider.getToolCallbacks()).map(c -> c.getToolDefinition().name()))
                .contains("list_accounts", "get_account", "create_account", "set_account_status");
        assertThat(tool("get_account").getToolDefinition().description()).isNotBlank();
    }

    /** Balance movements stay out of MCP: money moves only through transaction-service's rules. */
    @Test
    void balanceMovementToolsAreNotExposed() {
        assertThat(Arrays.stream(provider.getToolCallbacks()).map(c -> c.getToolDefinition().name()))
                .doesNotContain("debit_account", "credit_account", "debitAccount", "creditAccount");
    }

    @Test
    void createAccountToolPersistsThroughTheServiceLayer() {
        String result = call("create_account",
                "{\"customerReference\":\"MCP-1\",\"currency\":\"EUR\",\"initialBalance\":50}");

        assertThat(result).contains("MCP-1").contains("ACTIVE");
        assertThat(repository.findByCustomerReference("MCP-1")).singleElement()
                .satisfies(a -> assertThat(a.getBalance()).isEqualTo(50.0));
    }

    @Test
    void getAccountToolReturnsTheCurrentBalance() {
        call("create_account", "{\"customerReference\":\"MCP-2\",\"currency\":\"EUR\",\"initialBalance\":12.5}");
        UUID id = repository.findByCustomerReference("MCP-2").get(0).getId();

        assertThat(call("get_account", "{\"accountId\":\"" + id + "\"}")).contains("12.5").contains("EUR");
    }

    @Test
    void setAccountStatusToolBlocksTheAccount() {
        call("create_account", "{\"customerReference\":\"MCP-3\",\"currency\":\"EUR\",\"initialBalance\":0}");
        UUID id = repository.findByCustomerReference("MCP-3").get(0).getId();

        assertThat(call("set_account_status", "{\"accountId\":\"" + id + "\",\"status\":\"BLOCKED\"}"))
                .contains("BLOCKED");
        assertThat(repository.findById(id)).get()
                .satisfies(a -> assertThat(a.getStatus().name()).isEqualTo("BLOCKED"));
    }

    /** An assistant must hit the same validations as any other client. */
    @Test
    void businessRulesStillApplyThroughMcp() {
        String blank = call("create_account", "{\"customerReference\":\"\",\"currency\":\"EUR\"}");
        assertThat(blank).containsIgnoringCase("customerReference");
        assertThat(repository.findByCustomerReference("")).isEmpty();

        assertThat(call("get_account", "{\"accountId\":\"" + UUID.randomUUID() + "\"}"))
                .containsIgnoringCase("not found");
    }
}
