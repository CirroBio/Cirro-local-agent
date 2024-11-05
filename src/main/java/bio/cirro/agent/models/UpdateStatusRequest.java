package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.util.Map;

@Serdeable
@Builder
public record UpdateStatusRequest(
        Status status,
        String message,
        Map<String, Object> details
) {
}
