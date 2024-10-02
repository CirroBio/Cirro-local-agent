package bio.cirro.agent.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.time.Instant;

/**
 * Expected output of credentials program
 * <a href="https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-sourcing-external.html">Sourcing Credentials</a>
 */
@Serdeable
@Builder
public class AWSCredentials {
    @JsonProperty("Version")
    private static final int VERSION = 1;

    @JsonProperty("AccessKeyId")
    private String accessKeyId;

    @JsonProperty("SecretAccessKey")
    private String secretAccessKey;

    @JsonProperty("SessionToken")
    private String sessionToken;

    @JsonProperty("Expiration")
    private Instant expiration;
}
