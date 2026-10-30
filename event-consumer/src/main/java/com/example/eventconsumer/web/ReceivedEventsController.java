package com.example.eventconsumer.web;

import com.example.eventconsumer.event.ReceivedEventStore;
import com.example.eventconsumer.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only view of what has been consumed, for demonstration and troubleshooting. */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class ReceivedEventsController {

    private final ReceivedEventStore store;

    @GetMapping
    public List<TransactionCreatedEvent> recent() {
        return store.recent();
    }

    @GetMapping("/stats")
    public Stats stats() {
        return new Stats(store.recent().size(), store.duplicateCount());
    }

    public record Stats(int retained, long duplicates) {
    }
}
