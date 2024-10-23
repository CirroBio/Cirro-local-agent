package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record UpdateStatusRequest(
        Status status,
        String message
) {
}
