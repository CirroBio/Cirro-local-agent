package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.aws.AwsCredentials;
import bio.cirro.agent.aws.AwsTokenClient;
import bio.cirro.agent.messaging.AgentClientFactory;
import bio.cirro.agent.messaging.dto.AnalysisUpdateMessage;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.models.UpdateStatusRequest;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sts.StsClient;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Singleton
@Slf4j
public class ExecutionService {
    private final ExecutionRepository executionRepository;
    private final AgentConfig agentConfig;
    private final StsClient stsClient;
    private final AgentClientFactory agentClientFactory;

    public List<ExecutionDto> list() {
        return executionRepository.getAll().stream()
                .map(ExecutionDto::from)
                .toList();
    }

    public void updateStatus(String executionId, UpdateStatusRequest request) {
        var execution = executionRepository.get(executionId);
        execution.setStatus(request.status());
        execution.setFinishOutput(new ExecutionFinishOutput(request.message()));

        var socket = agentClientFactory.getClientSocket();
        if (!socket.isOpen()) {
            log.warn("Socket is closed, cannot send message");
            return;
        }
        // Build update message and send back to the server.
        var nativeJobId = Optional.ofNullable(execution.getStartOutput())
                .map(ExecutionStartOutput::localJobId)
                .orElse(null);
        var msg = AnalysisUpdateMessage.builder()
                .datasetId(execution.getDatasetId())
                .projectId(execution.getProjectId())
                .nativeJobId(nativeJobId)
                .status(request.status())
                .message(request.message())
                .details(request.details())
                .build();
        socket.sendMessage(msg);
    }

    public AwsCredentials generateS3Credentials(String executionId) {
        var execution = executionRepository.get(executionId);
        if (execution.getStatus() == Status.COMPLETED) {
            throw new IllegalStateException("Execution already completed");
        }

        var tokenClient = new AwsTokenClient(stsClient, execution.getFileAccessRoleArn(), agentConfig.getId());
        var creds = tokenClient.generateCredentialsForExecution(execution);
        return AwsCredentials.builder()
                .accessKeyId(creds.accessKeyId())
                .secretAccessKey(creds.secretAccessKey())
                .sessionToken(creds.sessionToken())
                .expiration(creds.expirationTime().orElse(null))
                .build();
    }
}
