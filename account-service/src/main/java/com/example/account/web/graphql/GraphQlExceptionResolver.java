package com.example.account.web.graphql;

import com.example.account.service.AccountNotActiveException;
import com.example.account.service.AccountNotFoundException;
import com.example.account.service.InsufficientFundsException;
import com.example.account.service.InvalidAccountException;
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
        if (ex instanceof AccountNotFoundException) {
            return error(ex, env, ErrorType.NOT_FOUND, "ACCOUNT_NOT_FOUND");
        }
        if (ex instanceof InvalidAccountException) {
            return error(ex, env, ErrorType.BAD_REQUEST, "INVALID_REQUEST");
        }
        if (ex instanceof AccountNotActiveException) {
            return error(ex, env, ErrorType.BAD_REQUEST, "ACCOUNT_NOT_ACTIVE");
        }
        if (ex instanceof InsufficientFundsException) {
            return error(ex, env, ErrorType.BAD_REQUEST, "INSUFFICIENT_FUNDS");
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
