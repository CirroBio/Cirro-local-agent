package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.AgentTokenService;
import bio.cirro.agent.client.AwsTokenClient;
import bio.cirro.agent.models.AWSCredentials;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.sts.StsClient;

import java.util.List;

@AllArgsConstructor
@Singleton
public class ExecutionSessionService {
    private final ExecutionRepository executionRepository;
    private final AgentConfig agentConfig;
    private final StsClient stsClient;
    private final AgentTokenService agentTokenService;

    public List<ExecutionSessionDto> list() {
        return executionRepository.getAll().stream()
                .map(ExecutionSessionDto::from)
                .toList();
    }

    public void completeExecution(String sessionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.removeSession(sessionId);
    }

    public AWSCredentials generateS3Credentials(String authorization) {
        var sessionId = agentTokenService.validate(authorization);
        var session = executionRepository.getSession(sessionId);
        var tokenClient = new AwsTokenClient(stsClient, session.getFileAccessRoleArn(), agentConfig.getId());
        var creds = tokenClient.generateCredentialsForExecutionSession(session);
        return AWSCredentials.builder()
                .accessKeyId(creds.accessKeyId())
                .secretAccessKey(creds.secretAccessKey())
                .sessionToken(creds.sessionToken())
                .expiration(creds.expirationTime().orElse(null))
                .build();
    }
}
