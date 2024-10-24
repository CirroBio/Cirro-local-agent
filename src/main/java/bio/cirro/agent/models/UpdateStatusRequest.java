package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;

import java.util.Map;

@Serdeable
public record UpdateStatusRequest(
        Status status,
        String message,
        Map<String, Object> details
) {
}
