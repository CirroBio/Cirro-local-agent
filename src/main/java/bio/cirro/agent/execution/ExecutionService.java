package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.AWSCredentials;
import bio.cirro.agent.utils.TokenClient;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@AllArgsConstructor
public class ExecutionService {
    private final AgentConfig agentConfig;
    private final TokenClient tokenClient;
    private final ExecutionRepository executionRepository;

    public ExecutionSession createSession(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var sessionId = UUID.randomUUID().toString();
        var session = ExecutionSession.builder()
                .sessionId(sessionId)
                .datasetId(runAnalysisCommandMessage.getDatasetId())
                .projectId(runAnalysisCommandMessage.getProjectId())
                .datasetS3(runAnalysisCommandMessage.getDatasetPath())
                .build();
        executionRepository.add(session);
        return session;
    }

    public AWSCredentials generateExecutionS3Credentials(String sessionId) {
        var session = executionRepository.getSession(sessionId);
        return tokenClient.generateCredentialsForExecutionSession(session);
    }

    public void completeExecution(String sessionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.removeSession(sessionId);
    }
}
