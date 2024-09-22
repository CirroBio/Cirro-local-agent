package bio.cirro.agent.utils;

import bio.cirro.agent.exception.AgentException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;

import java.util.concurrent.ExecutionException;


/**
 * Signs an HTTP request to API Gateway using AWS Signature Version 4.
 * Uses the default credentials provider chain to get the credentials.
 */
@Singleton
@Slf4j
public class AWSRequestSigner {
    public MutableHttpRequest<String> signRequest(MutableHttpRequest<String> request) {
        try (var credentialsProvider = DefaultCredentialsProvider.create()) {
            var identity = credentialsProvider.resolveIdentity().get();
            log.debug("Signing request with identity: {}", identity.accessKeyId());
            var body = request.getBody().orElse(null);
            // Convert to SDK request object
            var sdkHttpRequest = SdkHttpRequest.builder()
                    .method(SdkHttpMethod.valueOf(request.getMethod().name()))
                    .uri(request.getUri())
                    .headers(request.getHeaders().asMap())
                    .build();
            // Sign the request
            var signer = AwsV4HttpSigner.create();
            var signedSdkRequest = signer.sign(r ->
                    r.identity(identity)
                            .request(sdkHttpRequest)
                            .payload(body == null ? null : ContentStreamProvider.fromUtf8String(body))
                            .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "execute-api")
                            .putProperty(AwsV4HttpSigner.REGION_NAME, "us-west-2"))
                    .request();
            // Convert back to Micronaut request object
            return HttpRequest.create(request.getMethod(), request.getUri().toString())
                    .headers(headers -> signedSdkRequest.headers().forEach((k, v) -> headers.add(k, v.getFirst())))
                    .body(body);
        } catch (ExecutionException e) {
            log.error("Error getting credentials", e);
            return request;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted", e);
            return request;
        } catch (SdkException e) {
            throw new AgentException(e.getMessage());
        }
    }
}
