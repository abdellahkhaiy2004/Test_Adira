package com.example.assistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The assistant's own responsibilities: collecting the MCP tools, carrying the system prompt and keeping
 * one conversation separate from another. The LLM is stubbed, so no provider or API key is involved.
 */
class BankingAssistantServiceTest {

    /** Minimal ToolCallback standing in for one published over MCP. */
    private static ToolCallback toolNamed(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name + " description")
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return "{}";
            }
        };
    }

    private static ObjectProvider<ToolCallbackProvider> providersOf(ToolCallback... callbacks) {
        ToolCallbackProvider provider = () -> callbacks;
        return new ObjectProvider<>() {
            @Override
            public ToolCallbackProvider getObject() {
                return provider;
            }

            @Override
            public ToolCallbackProvider getObject(Object... args) {
                return provider;
            }

            @Override
            public ToolCallbackProvider getIfAvailable() {
                return provider;
            }

            @Override
            public ToolCallbackProvider getIfUnique() {
                return provider;
            }

            @Override
            public Stream<ToolCallbackProvider> stream() {
                return callbacks.length == 0 ? Stream.empty() : Stream.of(provider);
            }
        };
    }

    private BankingAssistantService assistantWith(StubChatModel model, ToolCallback... callbacks) {
        return new BankingAssistantService(ChatClient.builder(model), providersOf(callbacks));
    }

    @Test
    void mcpToolsAreDiscoveredAndExposed() {
        BankingAssistantService assistant = assistantWith(new StubChatModel(),
                toolNamed("get_account"), toolNamed("create_transaction"), toolNamed("transfer_money"));

        assertThat(assistant.availableTools())
                .containsExactly("create_transaction", "get_account", "transfer_money");
    }

    @Test
    void withoutMcpToolsTheAssistantStillAnswersButCanDoNoBanking() {
        StubChatModel model = new StubChatModel();
        model.reply("I cannot reach the banking services right now.");
        BankingAssistantService assistant = assistantWith(model);

        assertThat(assistant.availableTools()).isEmpty();
        assertThat(assistant.chat("c1", "What is my balance?"))
                .isEqualTo("I cannot reach the banking services right now.");
    }

    @Test
    void everyTurnCarriesTheBankingSystemPrompt() {
        StubChatModel model = new StubChatModel();
        assistantWith(model, toolNamed("get_account")).chat("c1", "hello");

        Message system = model.lastInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no system message in the prompt"));

        assertThat(system.getText())
                .contains("Use the provided tools")
                .contains("ask the user to confirm")
                .contains("never work around it or claim the operation succeeded");
    }

    @Test
    void historyIsKeptPerConversationAndCanBeReset() {
        StubChatModel model = new StubChatModel();
        BankingAssistantService assistant = assistantWith(model);

        assistant.chat("alice", "first");
        assistant.chat("alice", "second");

        List<Message> afterTwoTurns = model.lastInstructions();
        assertThat(afterTwoTurns).extracting(Message::getText).contains("first", "second");

        // A different conversation must not inherit Alice's messages.
        assistant.chat("bob", "hello");
        assertThat(model.lastInstructions()).extracting(Message::getText)
                .contains("hello").doesNotContain("first", "second");

        assistant.reset("alice");
        assistant.chat("alice", "third");
        assertThat(model.lastInstructions()).extracting(Message::getText)
                .contains("third").doesNotContain("first", "second");
    }
}
