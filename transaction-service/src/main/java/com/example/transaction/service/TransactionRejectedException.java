package com.example.transaction.service;

/** A business rule refused the request. {@link Reason} carries a stable code and the matching HTTP status. */
public class TransactionRejectedException extends RuntimeException {

    public enum Reason {
        INVALID_REQUEST(400),
        AMOUNT_EXCEEDS_MAX(400),
        CURRENCY_MISMATCH(422),
        ACCOUNT_NOT_ACTIVE(409),
        INSUFFICIENT_FUNDS(409);

        private final int httpStatus;

        Reason(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int httpStatus() {
            return httpStatus;
        }
    }

    private final Reason reason;

    public TransactionRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
