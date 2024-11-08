package bio.cirro.agent.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles exceptions thrown by the API.
 */
@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
@Slf4j
public class ApiExceptionHandler implements ExceptionHandler<RuntimeException, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, RuntimeException exception) {
        var status = switch (exception) {
            case SecurityException ignore -> HttpStatus.FORBIDDEN;
            case IllegalArgumentException ignore -> HttpStatus.BAD_REQUEST;
            case IllegalStateException ignore -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled exception", exception);
        }
        return HttpResponse.status(status).body(new ErrorMessage(exception.getMessage()));
    }

    @Serdeable
    public record ErrorMessage(String message) {
    }
}
