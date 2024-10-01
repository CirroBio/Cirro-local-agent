package bio.cirro.agent.utils;

import bio.cirro.agent.exception.AgentException;
import io.micronaut.http.HttpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AWSRequestSignerTest {
    AWSRequestSigner awsRequestSigner;

    @BeforeEach
    void setUp() {
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        awsRequestSigner = new AWSRequestSigner();
    }

    @Test
    void testSign() {
        var exampleKey = "EXAMPLE_KEY";
        System.setProperty("aws.accessKeyId", exampleKey);
        System.setProperty("aws.secretAccessKey", "test");

        var request = HttpRequest
                .GET("http://localhost:8080")
                .body("");
        var signedRequest = awsRequestSigner.signRequest(request, "us-west-2");
        var headers = signedRequest.getHeaders();
        Assertions.assertTrue(headers.contains("X-Amz-Date"));
        Assertions.assertTrue(headers.contains("Authorization"));
        Assertions.assertTrue(headers.contains("x-amz-content-sha256"));

        var authorization = headers.get("Authorization");
        Assertions.assertTrue(authorization.startsWith("AWS4-HMAC"));
        Assertions.assertTrue(authorization.contains(exampleKey));
        Assertions.assertTrue(authorization.contains("execute-api"));
    }

    @Test
    void testThrowsNoCredentials() {
        var request = HttpRequest
                .GET("http://localhost:8080")
                .body("");
        Assertions.assertThrows(AgentException.class, () -> awsRequestSigner.signRequest(request, "us-west-2"));
    }
}
