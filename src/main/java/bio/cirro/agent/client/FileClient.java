package bio.cirro.agent.client;

import bio.cirro.agent.utils.S3Path;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

public class FileClient implements AutoCloseable {
    private final S3Client s3Client;

    public FileClient(AwsCredentialsProvider credentialsProvider) {
        this.s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .crossRegionAccessEnabled(true)
                .build();
    }

    public String getObject(S3Path s3Path) throws IOException {
        return getObject(s3Path.getBucket(), s3Path.getKey());
    }

    public String getObject(String bucket, String key) throws IOException {
        return new String(
                s3Client
                    .getObject(r -> r.bucket(bucket).key(key))
                    .readAllBytes()
        );
    }

    @Override
    public void close() {
        s3Client.close();
    }
}
