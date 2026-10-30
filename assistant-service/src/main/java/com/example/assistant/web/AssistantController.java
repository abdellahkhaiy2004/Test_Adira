package com.example.assistant.web;

import com.example.assistant.service.BankingAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST client for the assistant (exam part E.2). */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final BankingAssistantService assistant;

    public record ChatRequest(String conversationId, String message) {
    }

    public record ChatResponse(String conversationId, String reply) {
    }

    public record ErrorResponse(int status, String code, String message) {
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        return new ChatResponse(conversationId, assistant.chat(conversationId, request.message()));
    }

    /** Which banking tools the assistant actually discovered over MCP. */
    @GetMapping("/tools")
    public List<String> tools() {
        return assistant.availableTools();
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable String conversationId) {
        assistant.reset(conversationId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_REQUEST", ex.getMessage()));
    }

    /** The model or a tool call failed. No banking state is implied either way. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleFailure(Exception ex) {
        log.error("Assistant turn failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "ASSISTANT_UNAVAILABLE",
                        "The assistant could not complete the request: " + ex.getMessage()));
    }
}
