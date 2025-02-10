package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Singleton
@Slf4j
public class ExecutionCleanupService {
    private final ExecutionRepository executionRepository;
    private final AgentConfig agentConfig;

    public void cleanupOldExecutions() {
        var executions = executionRepository.getAll();

        for (var execution : executions) {
            // TODO: Purge old executions
        }
    }
}
