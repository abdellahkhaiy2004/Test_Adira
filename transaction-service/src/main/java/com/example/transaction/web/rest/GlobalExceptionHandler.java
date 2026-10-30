package com.example.transaction.web.rest;

import com.example.transaction.client.AccountServiceUnavailableException;
import com.example.transaction.client.RemoteAccountNotFoundException;
import com.example.transaction.client.RemoteBusinessException;
import com.example.transaction.service.TransactionNotFoundException;
import com.example.transaction.service.TransactionRejectedException;
import com.example.transaction.web.rest.TransactionDtos.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionRejectedException.class)
    public ResponseEntity<ErrorResponse> handleRejected(TransactionRejectedException ex) {
        return build(ex.getReason().httpStatus(), ex.getReason().name(), ex.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException ex) {
        return build(404, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(RemoteAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(RemoteAccountNotFoundException ex) {
        return build(404, "ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    /** account-service refused at execution time (e.g. a concurrent debit emptied the account). */
    @ExceptionHandler(RemoteBusinessException.class)
    public ResponseEntity<ErrorResponse> handleRemoteRejected(RemoteBusinessException ex) {
        return build(409, "REJECTED_BY_ACCOUNT_SERVICE", ex.getMessage());
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(AccountServiceUnavailableException ex) {
        log.warn("account-service unavailable: {}", ex.getMessage());
        return build(503, "ACCOUNT_SERVICE_UNAVAILABLE",
                "Account service is unavailable; the operation was not applied");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(400, "INVALID_REQUEST", "Malformed JSON body or invalid field value");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(400, "INVALID_REQUEST", "Invalid value for parameter '" + ex.getName() + "'");
    }

    private ResponseEntity<ErrorResponse> build(int status, String code, String message) {
        HttpStatus http = HttpStatus.valueOf(status);
        return ResponseEntity.status(http).body(new ErrorResponse(status, http.getReasonPhrase(), code, message));
    }
}
