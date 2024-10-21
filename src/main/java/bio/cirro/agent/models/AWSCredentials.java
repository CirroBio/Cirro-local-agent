package bio.cirro.agent.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Expected output of credentials program
 * <a href="https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-sourcing-external.html">Sourcing Credentials</a>
 */
@Serdeable
@Builder
@Value
public class AWSCredentials {
    @JsonProperty("Version")
    int version = 1;

    @JsonProperty("AccessKeyId")
    String accessKeyId;

    @JsonProperty("SecretAccessKey")
    String secretAccessKey;

    @JsonProperty("SessionToken")
    String sessionToken;

    @JsonProperty("Expiration")
    Instant expiration;
}
