package bio.cirro.agent.utils;

import lombok.Getter;

import java.net.URI;
import java.nio.file.Paths;

@Getter
public class S3Path {

    private final String bucket;
    private final String key;

    public S3Path(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }

    public String toString() {
        return String.format("s3://%s/%s", bucket, key);
    }

    public S3Path resolve(String key) {
        return new S3Path(bucket, Paths.get(this.key, key).toString());
    }

    public static S3Path parse(String s3Path) {
        var s3Uri = URI.create(s3Path);
        if (!s3Uri.getScheme().equals("s3")) {
            throw new IllegalArgumentException("Invalid S3 path: " + s3Path);
        }
        var bucket = s3Uri.getAuthority();
        if (bucket == null) {
            throw new IllegalArgumentException("Invalid S3 path: " + s3Path);
        }
        var path = s3Uri.getPath();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 path: " + s3Path);
        }
        return new S3Path(bucket, path.substring(1));
    }
}
