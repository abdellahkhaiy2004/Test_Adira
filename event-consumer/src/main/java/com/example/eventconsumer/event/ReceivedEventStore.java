package com.example.eventconsumer.event;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded in-memory record of what was consumed, so the consumer can be observed without touching banking
 * data. It holds events only; nothing here is a source of truth for any balance.
 *
 * <p>Delivery is at-least-once, so the same event can arrive twice (rebalance, retry, offset replay).
 * {@link #record(TransactionCreatedEvent)} reports whether an eventId was already seen, which is how a real
 * consumer with side effects would stay idempotent. This one only logs, so a duplicate is harmless anyway.
 */
@Component
public class ReceivedEventStore {

    private static final int MAX_RETAINED = 200;

    private final Deque<TransactionCreatedEvent> received = new ArrayDeque<>();
    private final Set<UUID> seenEventIds = new HashSet<>();
    private long duplicateCount;

    /** Returns true when this eventId had never been seen before. */
    public synchronized boolean record(TransactionCreatedEvent event) {
        boolean firstTime = event.eventId() == null || seenEventIds.add(event.eventId());
        if (!firstTime) {
            duplicateCount++;
        }
        received.addFirst(event);
        while (received.size() > MAX_RETAINED) {
            received.removeLast();
        }
        return firstTime;
    }

    public synchronized List<TransactionCreatedEvent> recent() {
        return List.copyOf(received);
    }

    public synchronized long duplicateCount() {
        return duplicateCount;
    }

    public synchronized void clear() {
        received.clear();
        seenEventIds.clear();
        duplicateCount = 0;
    }
}
