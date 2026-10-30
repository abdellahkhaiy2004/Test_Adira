package com.example.transaction.service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo actions for remote balance changes already applied. The remote service is not part of the local database
 * transaction, so when a later step fails the earlier remote effects are reversed here (last in, first out).
 */
@Slf4j
final class Compensations {

    private final Deque<Runnable> steps = new ArrayDeque<>();

    void add(Runnable undo) {
        steps.push(undo);
    }

    void runAll() {
        while (!steps.isEmpty()) {
            try {
                steps.pop().run();
            } catch (RuntimeException e) {
                log.error("COMPENSATION FAILED - balances may be inconsistent, manual reconciliation required", e);
            }
        }
    }
}
