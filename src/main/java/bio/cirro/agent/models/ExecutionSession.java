package bio.cirro.agent.models;

import bio.cirro.agent.utils.S3Path;
import lombok.Builder;

@Builder
public record ExecutionSession(
        String datasetId,
        String projectId,
        String localJobId,
        String username,
        String datasetS3,
        CloudAccount projectAccount
) {
    public S3Path datasetS3Path() {
        return S3Path.parse(datasetS3);
    }
}
