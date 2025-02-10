package bio.cirro.agent.execution;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ExecutionStartOutput(
        String stdout,
        String localJobId
) {
}
