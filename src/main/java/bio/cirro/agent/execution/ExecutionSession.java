package bio.cirro.agent.execution;

import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.utils.S3Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.nio.file.Path;


@AllArgsConstructor
@Data
@Builder
public class ExecutionSession {
    private final String sessionId;
    private final Path workingDirectory;
    private final RunAnalysisCommandMessage messageData;

    private ExecutionSessionOutput output;
    private AwsCredentials awsCredentials;

    public String getDatasetId() {
        return messageData.getDatasetId();
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

    public S3Path getParamsS3Path() {
        return S3Path.parse(messageData.getParameterFilePath());
    }

    public S3Path getDatasetS3Path() {
        return S3Path.parse(messageData.getDatasetPath());
    }

    public Path getNextflowConfigPath() {
        return workingDirectory.resolve("nextflow.config");
    }

    public Path getParamsPath() {
        return workingDirectory.resolve("params.json");
    }

    public Path getEnvironmentPath() {
        return workingDirectory.resolve("environment.json");
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

    public AwsCredentialsProvider getAwsCredentialsProvider() {
        return () -> awsCredentials;
    }
}
