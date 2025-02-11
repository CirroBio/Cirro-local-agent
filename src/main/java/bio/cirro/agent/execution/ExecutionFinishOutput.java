package bio.cirro.agent.execution;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ExecutionFinishOutput(
        String message
) {
}
