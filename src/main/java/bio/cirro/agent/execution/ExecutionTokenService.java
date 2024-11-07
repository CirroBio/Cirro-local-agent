package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.aws.AwsCredentials;
import bio.cirro.agent.aws.AwsTokenClient;
import bio.cirro.agent.models.Status;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sts.StsClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class ExecutionTokenService {
    private final ExecutionRepository executionRepository;
    private final StsClient stsClient;
    private final AgentConfig agentConfig;
    private final Map<String, AwsCredentials> executionCredentialsCache;

    public ExecutionTokenService(ExecutionRepository executionRepository, StsClient stsClient, AgentConfig agentConfig) {
        this.executionRepository = executionRepository;
        this.stsClient = stsClient;
        this.agentConfig = agentConfig;
        this.executionCredentialsCache = new ConcurrentHashMap<>();
    }

    public AwsCredentials generateS3Credentials(String executionId) {
        var execution = executionRepository.get(executionId);
        if (execution.getStatus() == Status.COMPLETED) {
            throw new IllegalStateException("Execution already completed");
        }

        if (executionCredentialsCache.containsKey(executionId)) {
            var cachedCreds = executionCredentialsCache.get(executionId);
            if (cachedCreds.getExpiration() != null &&
                    cachedCreds.getExpiration().isAfter(Instant.now())) {
                log.debug("Using cached S3 credentials for execution: {}", executionId);
                return cachedCreds;
            }
        }

        log.debug("Generating S3 credentials for execution: {}", executionId);
        var tokenClient = new AwsTokenClient(stsClient, execution.getFileAccessRoleArn(), agentConfig.getId());
        var creds = tokenClient.generateCredentialsForExecution(execution);
        var credsResponse = AwsCredentials.builder()
                .accessKeyId(creds.accessKeyId())
                .secretAccessKey(creds.secretAccessKey())
                .sessionToken(creds.sessionToken())
                .expiration(creds.expirationTime().orElse(null))
                .build();
        executionCredentialsCache.put(executionId, credsResponse);
        return credsResponse;
    }
}
