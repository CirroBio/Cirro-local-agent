package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record SystemInfoResponse(
        String agentEndpoint,
        String region
) {
}
