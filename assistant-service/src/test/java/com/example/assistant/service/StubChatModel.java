package com.example.assistant.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/** Stands in for the LLM so the assistant's wiring can be tested without a provider or an API key. */
class StubChatModel implements ChatModel {

    private final List<Prompt> receivedPrompts = new ArrayList<>();
    private String reply = "ok";

    @Override
    public ChatResponse call(Prompt prompt) {
        receivedPrompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().build();
    }

    void reply(String reply) {
        this.reply = reply;
    }

    List<Prompt> receivedPrompts() {
        return receivedPrompts;
    }

    Prompt lastPrompt() {
        return receivedPrompts.get(receivedPrompts.size() - 1);
    }

    List<Message> lastInstructions() {
        return lastPrompt().getInstructions();
    }
}
