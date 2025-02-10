package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.models.Status;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@AllArgsConstructor
@Singleton
@Slf4j
public class ExecutionCleanupService {
    private final ExecutionRepository executionRepository;
    private final AgentConfig agentConfig;

    public void cleanupOldExecutions() {
        log.info("Cleaning up old executions");
        var executions = executionRepository.getAll();
        var threshold = Instant.now().minus(agentConfig.cleanupThresholdDays());

        for (var execution : executions) {
            if (execution.getStatus() == Status.RUNNING || execution.getStatus() == Status.PENDING) {
                log.debug("Skipping running execution: {}", execution.getDatasetId());
                continue;
            }

            if (threshold.isAfter(execution.getCreatedAt())) {
                log.info("Cleaning up execution: {}", execution.getDatasetId());
                executionRepository.remove(execution.getDatasetId());
            }
        }
        log.debug("Finished cleaning up old executions");
    }
}
