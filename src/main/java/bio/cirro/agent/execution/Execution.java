package bio.cirro.agent.execution;

import bio.cirro.agent.aws.S3Path;
import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single execution of an analysis or job
 */
@AllArgsConstructor
@Data
@Builder
@Slf4j
public class Execution {
    private final Path agentWorkingDirectory;
    private final Path agentSharedDirectory;
    private final RunAnalysisCommandMessage messageData;
    private final Instant createdAt;
    private Status status;
    private ExecutionStartOutput startOutput;
    private ExecutionFinishOutput finishOutput;
    private Instant finishedAt;

    public String getDatasetId() {
        return messageData.getDatasetId();
    }

    public String getExecutionId() {
        return getDatasetId();
    }

    public String getProjectId() {
        return messageData.getProjectId();
    }

    public String getUsername() {
        return messageData.getUsername();
    }

    public String getFileAccessRoleArn() {
        return messageData.getFileAccessRoleArn();
    }

    public S3Path getDatasetS3Path() {
        return S3Path.parse(messageData.getDatasetPath());
    }

    /**
     * Working directory of the execution, this is where all the scripts are run under.
     */
    public Path getWorkingDirectory() {
        return getProjectRoot().resolve(
                String.format("datasets/%s", getDatasetId())
        );
    }

    /**
     * Root of the project directory, this is where the executors work directory is stored
     */
    public Path getProjectRoot() {
        return agentWorkingDirectory.resolve(
                String.format("projects/%s", getProjectId())
        );
    }

    /**
     * Path to the environment file that contains all the environment variables for the execution
     */
    public Path getEnvironmentPath() {
        return getWorkingDirectory().resolve("env.list");
    }

    /**
     * Generates the environment variables for the execution
     */
    public Map<String, String> getEnvironment(String token, String agentEndpoint) {
        // Add any variables injected from Cirro
        var environment = Optional.ofNullable(messageData.getEnvironment())
                .map(HashMap::new)
                .orElse(new HashMap<>());
        // Add variables from the agent
        environment.put("PW_PROJECT_DIR", getProjectRoot().toString());
        environment.put("PW_WORKING_DIR", getWorkingDirectory().toString());
        environment.put("PW_SHARED_DIR", getAgentSharedDirectory().toString());
        environment.put("PW_ENVIRONMENT_FILE", getEnvironmentPath().toString());
        environment.put("PW_USERNAME", getUsername());
        // Write variables needed to call AWS credentials service
        environment.put("AGENT_TOKEN", token);
        environment.put("AGENT_ENDPOINT", agentEndpoint);
        environment.put("AGENT_EXECUTION_ID", getDatasetId());
        environment.put("AWS_PROFILE", "default");
        environment.put("AWS_REGION", getMessageData().getRegion());
        environment.put("AWS_CONFIG_FILE", getAwsConfigPath().toString());
        environment.put("AWS_SHARED_CREDENTIALS_FILE", getAwsCredentialsFilePath().toString());
        // Escape all values
        environment.replaceAll((k, v) -> StringEscapeUtils.escapeXSI(v));
        return Map.copyOf(environment);
    }

    public Path getAwsConfigPath() {
        return getWorkingDirectory().resolve("aws.config");
    }

    public Path getAwsCredentialsFilePath() {
        return getWorkingDirectory().resolve("aws.credentials");
    }

    public Path getCredentialsHelperPath() {
        return getWorkingDirectory().resolve("credentials-helper.sh");
    }
}
