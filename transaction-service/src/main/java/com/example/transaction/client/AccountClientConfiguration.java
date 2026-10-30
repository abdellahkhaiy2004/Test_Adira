package com.example.transaction.client;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Feign configuration for {@link AccountClient} (deliberately not annotated @Configuration, so it only
 * applies to this client). Translates account-service business errors into typed exceptions.
 */
public class AccountClientConfiguration {

    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Bean
    public ErrorDecoder accountErrorDecoder() {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            int status = response.status();
            if (status == 404) {
                return new RemoteAccountNotFoundException(messageOf(response));
            }
            if (status == 400 || status == 409) {
                return new RemoteBusinessException(status, messageOf(response));
            }
            return fallback.decode(methodKey, response);
        };
    }

    private static String messageOf(Response response) {
        if (response.body() == null) {
            return "rejected by account-service (HTTP " + response.status() + ")";
        }
        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            Matcher m = MESSAGE.matcher(Util.toString(reader));
            return m.find() ? m.group(1) : "rejected by account-service (HTTP " + response.status() + ")";
        } catch (IOException e) {
            return "rejected by account-service (HTTP " + response.status() + ")";
        }
    }
}
