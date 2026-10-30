package com.example.transaction.client;

/** account-service refused the operation for a business reason (400/409). Not an availability failure. */
public class RemoteBusinessException extends RuntimeException {

    private final int status;

    public RemoteBusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
