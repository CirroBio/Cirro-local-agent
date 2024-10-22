package bio.cirro.agent.execution;

import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.utils.S3Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@AllArgsConstructor
@Data
@Builder
@Slf4j
public class Execution {
    private static final List<String> ALLOWED_ENV_PREFIXES = List.of("PW_", "CIRRO_");

    private final Path workingDirectory;
    private final RunAnalysisCommandMessage messageData;
    private final Status status;
    private final Instant createdAt;

    private ExecutionStartOutput output;

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

    public Path getEnvironmentPath() {
        return workingDirectory.resolve("environment.sh");
    }

    public Map<String, String> getEnvironment() {
        // Add any variables injected by Cirro
        var environment = Optional.ofNullable(messageData.getEnvironment())
                .orElse(new HashMap<>());
        for (var variable : environment.entrySet()) {
            // Check if variable is allowed to be set
            if (ALLOWED_ENV_PREFIXES.stream().noneMatch(variable.getKey()::startsWith)) {
                log.warn("Setting of environment variable {} not allowed", variable.getKey());
                continue;
            }

            environment.put(variable.getKey(), StringEscapeUtils.escapeXSI(variable.getValue()));
        }

        environment.put("AWS_CONFIG_FILE", getAwsConfigPath().toString());
        environment.put("AWS_SHARED_CREDENTIALS_FILE", getAwsCredentialsPath().toString());
        environment.put("CIRRO_WORKING_DIR", workingDirectory.toString());
        return new HashMap<>(environment);
    }
    public Path getAwsConfigPath() {
        return workingDirectory.resolve("aws.config");
    }

    public Path getAwsCredentialsPath() {
        return workingDirectory.resolve("aws.credentials");
    }

    public Path getCredentialsHelperPath() {
        return workingDirectory.resolve("credentials-helper.sh");
    }
}
