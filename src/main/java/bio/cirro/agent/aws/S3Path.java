package bio.cirro.agent.aws;

import java.net.URI;

public record S3Path(String bucket, String key) {

    public String toString() {
        return String.format("s3://%s/%s", bucket, key);
    }

    public static S3Path parse(String s3Path) {
        var s3Uri = URI.create(s3Path);
        if (!s3Uri.getScheme().equals("s3")) {
            throwInvalidS3Path(s3Path);
        }
        var bucket = s3Uri.getAuthority();
        if (bucket == null) {
            throwInvalidS3Path(s3Path);
        }
        var path = s3Uri.getPath();
        if (path.isEmpty()) {
            throwInvalidS3Path(s3Path);
        }
        return new S3Path(bucket, path.substring(1));
    }

    private static void throwInvalidS3Path(String s3Path) {
        throw new IllegalArgumentException("Invalid S3 path: " + s3Path);
    }
}
