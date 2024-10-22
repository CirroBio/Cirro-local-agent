package bio.cirro.agent.execution;

import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage location for executions
 * Clears upon agent restart
 * Consider changing to SQLite database
 */
@Singleton
@AllArgsConstructor
public class ExecutionRepository {
    private static final Map<String, Execution> executionMap = new ConcurrentHashMap<>();

    public void add(Execution execution) {
        executionMap.put(execution.getExecutionId(), execution);
    }

    public List<Execution> getAll() {
        return List.copyOf(executionMap.values());
    }

    public Execution get(String executionId) {
        var execution = executionMap.get(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Invalid execution ID");
        }
        return execution;
    }

    public void remove(String executionId) {
        executionMap.remove(executionId);
    }
}
