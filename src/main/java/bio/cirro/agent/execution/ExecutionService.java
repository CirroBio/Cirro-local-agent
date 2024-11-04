package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.aws.AwsCredentials;
import bio.cirro.agent.aws.AwsTokenClient;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.messaging.AgentClientFactory;
import bio.cirro.agent.messaging.dto.AnalysisUpdateMessage;
import bio.cirro.agent.messaging.dto.StopAnalysisMessage;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.models.UpdateStatusRequest;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sts.StsClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    public void stopExecution(StopAnalysisMessage stopAnalysisMessage) {
        var execution = executionRepository.get(stopAnalysisMessage.getDatasetId());

        try {
            Path stopScript = agentConfig.getStopScript();
            if (!stopScript.toFile().exists()) {
                throw new ExecutionException("Stop script not found");
            }
            var stopProcessBuilder = new ProcessBuilder()
                    .directory(execution.getWorkingDirectory().toFile())
                    .command(stopScript.toAbsolutePath().toString())
                    .redirectErrorStream(true);
            var env = stopProcessBuilder.environment();
            env.put("PW_ENVIRONMENT_FILE", execution.getEnvironmentPath().toString());
            env.put("PW_JOB_ID", execution.getStartOutput().localJobId());

            var stopProcess = stopProcessBuilder.start();
            var output = stopProcess.getInputStream().readAllBytes();
            log.debug("Stop execution output: {}", output);

            stopProcess.waitFor(10, TimeUnit.SECONDS);
            if (stopProcess.exitValue() != 0) {
                throw new ExecutionException("Failed to stop execution");
            }
            stopProcess.destroy();
        } catch (IOException e) {
            throw new ExecutionException("Failed to stop execution", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
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
