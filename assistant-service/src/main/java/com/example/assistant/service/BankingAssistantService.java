package com.example.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The conversational layer. It owns the dialogue, never the banking rules: every banking action is performed
 * by calling an MCP tool published by account-service or transaction-service, which validate the request
 * exactly as they do for a REST caller. The assistant holds no repository, no database and no balance logic.
 */
@Slf4j
@Service
public class BankingAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the assistant of a retail bank. You help users consult accounts, review transaction
            history, record transactions and transfer money.

            Rules you must follow:
            - Use the provided tools for every piece of banking information or action. Never invent an
              account, a balance, an amount or a transaction id, and never answer from memory of an earlier
              turn when a fresh value is needed: call the tool again.
            - Before any operation that moves money (creating a transaction or a transfer), restate the
              amount, currency and the account(s) involved, and ask the user to confirm. Only call the tool
              after an explicit confirmation.
            - If a tool reports an error or a refusal, explain the reason to the user in plain language and
              do not retry with different values unless the user asks. A refusal is a decision of the bank's
              business rules; never work around it or claim the operation succeeded.
            - You cannot change the bank's limits, unblock an account on your own authority, or perform any
              operation for which no tool exists. Say so plainly.
            - Politely decline anything unrelated to this bank's services.
            """;

    private final ChatClient chatClient;
    private final List<String> availableTools;
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    public BankingAssistantService(ChatClient.Builder chatClientBuilder,
                                   ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        ToolCallback[] callbacks = toolCallbackProviders.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);

        this.availableTools = Arrays.stream(callbacks).map(c -> c.getToolDefinition().name()).sorted().toList();
        log.info("Banking assistant started with {} MCP tool(s): {}", availableTools.size(), availableTools);
        if (callbacks.length == 0) {
            log.warn("No MCP tools available: the assistant can converse but cannot perform any banking "
                    + "operation. Check that account-service and transaction-service are reachable.");
        }

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(callbacks)
                .build();
    }

    /** Tool names discovered over MCP, exposed so the wiring can be inspected without talking to the model. */
    public List<String> availableTools() {
        return availableTools;
    }

    /**
     * Runs one conversational turn. The model may call banking tools during this call; their results are fed
     * back to it automatically before the final answer is produced.
     */
    public String chat(String conversationId, String userMessage) {
        List<Message> history = conversations.computeIfAbsent(conversationId,
                id -> Collections.synchronizedList(new ArrayList<>()));

        List<Message> prompt;
        synchronized (history) {
            history.add(new UserMessage(userMessage));
            prompt = List.copyOf(history);
        }

        String answer = chatClient.prompt().messages(prompt).call().content();

        synchronized (history) {
            history.add(new AssistantMessage(answer == null ? "" : answer));
        }
        return answer;
    }

    public void reset(String conversationId) {
        conversations.remove(conversationId);
    }
}
