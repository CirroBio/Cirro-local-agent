package bio.cirro.agent.models;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CloudAccount(
        String region,
        String partition,
        String accountId
) {
}
