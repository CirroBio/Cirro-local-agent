package bio.cirro.agent.utils;

import bio.cirro.agent.aws.AwsCredentials;
import io.micronaut.serde.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

@Slf4j
public class AwsCredentialsTest {
    @Test
    public void testGenerateCredentialsForProject() throws IOException {
        var test = AwsCredentials.builder()
                .accessKeyId("access")
                .secretAccessKey("secret")
                .sessionToken("token")
                .expiration(Instant.now())
                .build();
        var mapper = ObjectMapper.getDefault();
        var str = mapper.writeValueAsString(test);
        str.toString();
    }
}
