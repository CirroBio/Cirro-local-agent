package bio.cirro.agent.utils;

import bio.cirro.agent.models.AWSCredentials;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.sts.StsClient;

@Singleton
@AllArgsConstructor
public class TokenClient {
    private final StsClient stsClient;

    @Factory
    public static TokenClient create() {
        return new TokenClient(StsClient.create());
    }

    public String getCurrentIdentity() {
        return stsClient.getCallerIdentity().arn();
    }

    public AWSCredentials generateCredentialsForProject(String roleArn) {
        var token = stsClient.assumeRole(
                r -> r.roleArn(roleArn)
                        .roleSessionName("ProjectRoleSession")
                        .durationSeconds(900)
        );

        return AWSCredentials.builder()
                .accessKeyId(token.credentials().accessKeyId())
                .secretAccessKey(token.credentials().secretAccessKey())
                .sessionToken(token.credentials().sessionToken())
                .expiration(token.credentials().expiration())
                .build();
    }
}
