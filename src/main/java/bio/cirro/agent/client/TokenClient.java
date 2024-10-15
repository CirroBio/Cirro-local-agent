package bio.cirro.agent.client;

import bio.cirro.agent.execution.ExecutionSession;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

import java.time.Duration;

@AllArgsConstructor
public class TokenClient {
    private static final int TOKEN_LIFETIME = (int) Duration.ofHours(1).getSeconds();
    private static final int MAX_ROLE_SESSION_NAME_LENGTH = 64;
    private final StsClient stsClient;
    private final String roleArn;
    private final String agentId;

    public AwsSessionCredentials generateCredentialsForExecutionSession(ExecutionSession executionSession) {
        var sessionPolicy = createPolicyForExecutionSession(executionSession);
        var response = stsClient.assumeRole(
                r -> r.roleArn(roleArn)
                        .roleSessionName(generateRoleSessionName(executionSession.getUsername()))
                        .policy(sessionPolicy.toJson())
                        .durationSeconds(TOKEN_LIFETIME)
                        .externalId(executionSession.getProjectId())
        );
        return AwsSessionCredentials.builder()
                .accessKeyId(response.credentials().accessKeyId())
                .secretAccessKey(response.credentials().secretAccessKey())
                .sessionToken(response.credentials().sessionToken())
                .expirationTime(response.credentials().expiration())
                .build();
    }

    private String generateRoleSessionName(String username) {
        var roleSessionName = String.format("%s-%s", agentId, username);
        return roleSessionName.substring(0, Math.min(roleSessionName.length(), MAX_ROLE_SESSION_NAME_LENGTH));
    }

    private IamPolicy createPolicyForExecutionSession(ExecutionSession executionSession) {
        var datasetS3Path = executionSession.getDatasetS3Path();
        var bucketArn = Arn.builder()
                .partition("aws")
                .service(S3Client.SERVICE_NAME)
                .resource(datasetS3Path.getBucket())
                .build()
                .toString();

        return IamPolicy.builder()
                .addStatement(b -> b
                        .sid("AllowListBucket")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:ListBucket")
                        .addAction("s3:GetBucketLocation")
                        .addResource("*")
                )
                .addStatement(b -> b
                        .sid("AllowWriteToDataset")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:PutObject")
                        .addAction("s3:DeleteObject")
                        .addResource(String.format("%s/%s/*", bucketArn, datasetS3Path.getKey()))
                )
                .addStatement(b -> b
                        .sid("AllowRead")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:GetObject*")
                        .addResource("*")
                )
                .addStatement(b -> b
                        .sid("AllowPullImage")
                        .effect(IamEffect.ALLOW)
                        .addAction("ecr:GetAuthorizationToken")
                        .addAction("ecr:BatchCheckLayerAvailability")
                        .addAction("ecr:GetDownloadUrlForLayer")
                        .addResource("*")
                )
                .build();
    }
}
