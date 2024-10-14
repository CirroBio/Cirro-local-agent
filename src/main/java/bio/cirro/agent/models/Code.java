package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Code(
        String uri,
        String version,
        String script
) {
}
