package com.example.transaction.web.graphql;

import com.example.transaction.client.AccountServiceUnavailableException;
import com.example.transaction.client.RemoteAccountNotFoundException;
import com.example.transaction.client.RemoteBusinessException;
import com.example.transaction.service.TransactionNotFoundException;
import com.example.transaction.service.TransactionRejectedException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Turns the service layer's business exceptions into GraphQL errors carrying the same codes as REST. */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof TransactionRejectedException rejected) {
            return error(ex, env, ErrorType.BAD_REQUEST, rejected.getReason().name());
        }
        if (ex instanceof TransactionNotFoundException) {
            return error(ex, env, ErrorType.NOT_FOUND, "NOT_FOUND");
        }
        if (ex instanceof RemoteAccountNotFoundException) {
            return error(ex, env, ErrorType.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        }
        if (ex instanceof RemoteBusinessException) {
            return error(ex, env, ErrorType.BAD_REQUEST, "REJECTED_BY_ACCOUNT_SERVICE");
        }
        if (ex instanceof AccountServiceUnavailableException) {
            return error(ex, env, ErrorType.INTERNAL_ERROR, "ACCOUNT_SERVICE_UNAVAILABLE");
        }
        return null;
    }

    private static GraphQLError error(Throwable ex, DataFetchingEnvironment env, ErrorType type, String code) {
        return GraphQLError.newError()
                .errorType(type)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .extensions(Map.of("code", code))
                .build();
    }
}
