package bio.cirro.agent.execution;

public record ExecutionSessionOutput(
        String stdout,
        String localJobId
) {
}
