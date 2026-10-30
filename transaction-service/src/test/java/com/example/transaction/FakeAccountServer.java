package com.example.transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal stand-in for account-service speaking the same HTTP contract (GET account, POST debit/credit,
 * same error codes). Lets tests exercise the real Feign + Resilience4J stack over HTTP without new dependencies.
 */
public class FakeAccountServer implements AutoCloseable {

    private static final Pattern PATH = Pattern.compile("/api/accounts/([0-9a-f-]{36})(/debit|/credit)?");
    private static final Pattern AMOUNT = Pattern.compile("\"amount\"\\s*:\\s*([0-9.]+)");

    private static final class Account {
        String status;
        final String currency;
        BigDecimal balance;

        Account(String status, String currency, BigDecimal balance) {
            this.status = status;
            this.currency = currency;
            this.balance = balance;
        }
    }

    private final HttpServer server;
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile boolean failDebit;
    private volatile boolean failCredit;
    private volatile boolean failEverything;
    private final java.util.Set<UUID> rejectedCreditTargets = ConcurrentHashMap.newKeySet();

    public FakeAccountServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public UUID addAccount(String status, String currency, String balance) {
        UUID id = UUID.randomUUID();
        accounts.put(id, new Account(status, currency, new BigDecimal(balance)));
        return id;
    }

    public double balanceOf(UUID id) {
        return accounts.get(id).balance.doubleValue();
    }

    public int requestCount() {
        return requestCount.get();
    }

    public void failDebit(boolean value) {
        this.failDebit = value;
    }

    public void failCredit(boolean value) {
        this.failCredit = value;
    }

    public void failEverything(boolean value) {
        this.failEverything = value;
    }

    /** Credits to this account answer 409 ACCOUNT_NOT_ACTIVE, even though its GET still says ACTIVE. */
    public void rejectCreditsTo(UUID id) {
        rejectedCreditTargets.add(id);
    }

    public void reset() {
        failDebit = false;
        failCredit = false;
        failEverything = false;
        rejectedCreditTargets.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws IOException {
        requestCount.incrementAndGet();
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Matcher path = PATH.matcher(ex.getRequestURI().getPath());
        if (failEverything || !path.matches()) {
            send(ex, 500, "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"boom\"}");
            return;
        }
        UUID id = UUID.fromString(path.group(1));
        String op = path.group(2);
        Account acc = accounts.get(id);
        if (acc == null) {
            error(ex, 404, "ACCOUNT_NOT_FOUND", "Account not found: " + id);
            return;
        }
        if (op == null) {
            send(ex, 200, json(id, acc));
            return;
        }
        if ((op.equals("/debit") && failDebit) || (op.equals("/credit") && failCredit)) {
            send(ex, 500, "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"boom\"}");
            return;
        }
        if (op.equals("/credit") && rejectedCreditTargets.contains(id)) {
            error(ex, 409, "ACCOUNT_NOT_ACTIVE", "Account " + id + " is not ACTIVE");
            return;
        }
        Matcher amount = AMOUNT.matcher(body);
        if (!amount.find()) {
            error(ex, 400, "INVALID_REQUEST", "amount must be strictly positive");
            return;
        }
        BigDecimal value = new BigDecimal(amount.group(1));
        synchronized (acc) {
            if (!"ACTIVE".equals(acc.status)) {
                error(ex, 409, "ACCOUNT_NOT_ACTIVE", "Account " + id + " is not ACTIVE");
                return;
            }
            if (op.equals("/debit")) {
                if (acc.balance.compareTo(value) < 0) {
                    error(ex, 409, "INSUFFICIENT_FUNDS", "Insufficient funds on account " + id);
                    return;
                }
                acc.balance = acc.balance.subtract(value);
            } else {
                acc.balance = acc.balance.add(value);
            }
            send(ex, 200, json(id, acc));
        }
    }

    private static String json(UUID id, Account a) {
        return "{\"id\":\"" + id + "\",\"customerReference\":\"C\",\"status\":\"" + a.status
                + "\",\"currency\":\"" + a.currency + "\",\"balance\":" + a.balance.doubleValue() + "}";
    }

    private static void error(HttpExchange ex, int status, String code, String message) throws IOException {
        send(ex, status, "{\"status\":" + status + ",\"error\":\"x\",\"code\":\"" + code
                + "\",\"message\":\"" + message + "\"}");
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
