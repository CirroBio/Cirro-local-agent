package bio.cirro.agent.execution;

import bio.cirro.agent.models.CloudAccount;
import bio.cirro.agent.utils.S3Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;


@AllArgsConstructor
@Data
@Builder
public class ExecutionSession {
    private final String sessionId;
    private final Path workingDirectory;
    private final String datasetId;
    private final String projectId;
    private final String username;
    private final String datasetS3;
    private final CloudAccount projectAccount;
    private String localJobId;

    public S3Path datasetS3Path() {
        return S3Path.parse(datasetS3);
    }
}
