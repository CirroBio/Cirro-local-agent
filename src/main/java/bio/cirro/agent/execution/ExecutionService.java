package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.aws.AwsCredentials;
import bio.cirro.agent.aws.AwsTokenClient;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.sts.StsClient;

import java.util.List;

@AllArgsConstructor
@Singleton
public class ExecutionService {
    private final ExecutionRepository executionRepository;
    private final AgentConfig agentConfig;
    private final StsClient stsClient;

    public List<ExecutionDto> list() {
        return executionRepository.getAll().stream()
                .map(ExecutionDto::from)
                .toList();
    }

    public void completeExecution(String executionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.remove(executionId);
    }

    public AwsCredentials generateS3Credentials(String executionId) {
        var execution = executionRepository.get(executionId);
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
