package bio.cirro.agent.execution;

public record ExecutionStartOutput(
        String stdout,
        String localJobId
) {
}
