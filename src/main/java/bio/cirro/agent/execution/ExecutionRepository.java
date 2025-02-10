package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.Status;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Storage location for executions
 */
@Singleton
@AllArgsConstructor
public class ExecutionRepository {
    private final ExecutionDataRepository executionDataRepository;
    private final ObjectMapper objectMapper;
    private final AgentConfig agentConfig;

    public Execution getNew(RunAnalysisCommandMessage messageData) {
        return Execution.builder()
                .messageData(messageData)
                .agentWorkingDirectory(agentConfig.getAbsoluteWorkDirectory())
                .agentSharedDirectory(agentConfig.getAbsoluteSharedDirectory())
                .status(Status.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public void add(Execution execution) {
        var executionData = toData(execution);
        executionDataRepository.save(executionData);
    }

    public List<Execution> getAll() {
        var executionDataList = executionDataRepository.findAll();
        return executionDataList.stream()
                .map(this::fromData)
                .toList();
    }

    public Execution get(String executionId) {
        var executionData = executionDataRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
        return fromData(executionData);
    }

    public void update(Execution execution) {
        var executionData = toData(execution);
        executionDataRepository.update(executionData);
    }

    public void remove(String executionId) {
        executionDataRepository.deleteById(executionId);
    }

    private ExecutionData toData(Execution execution) {
        return ExecutionData.builder()
                .id(execution.getExecutionId())
                .messageDataJson(toJson(execution.getMessageData()))
                .status(execution.getStatus())
                .startOutputJson(toJson(execution.getStartOutput()))
                .finishOutputJson(toJson(execution.getFinishOutput()))
                .createdAt(execution.getCreatedAt())
                .finishedAt(execution.getFinishedAt())
                .build();
    }

    private Execution fromData(ExecutionData executionData) {
        return Execution.builder()
                .agentWorkingDirectory(agentConfig.getAbsoluteWorkDirectory())
                .agentSharedDirectory(agentConfig.getAbsoluteSharedDirectory())
                .messageData(fromJson(executionData.getMessageDataJson(), RunAnalysisCommandMessage.class))
                .createdAt(executionData.getCreatedAt())
                .status(executionData.getStatus())
                .startOutput(fromJson(executionData.getStartOutputJson(), ExecutionStartOutput.class))
                .finishOutput(fromJson(executionData.getFinishOutputJson(), ExecutionFinishOutput.class))
                .finishedAt(executionData.getFinishedAt())
                .build();
    }

    private String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize object", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize object", e);
        }
    }
}
