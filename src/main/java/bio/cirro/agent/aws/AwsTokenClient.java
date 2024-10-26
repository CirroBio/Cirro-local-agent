package bio.cirro.agent.aws;

import bio.cirro.agent.execution.Execution;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

import java.time.Duration;

@AllArgsConstructor
public class AwsTokenClient {
    private static final int TOKEN_LIFETIME = (int) Duration.ofHours(1).getSeconds();
    private static final int MAX_ROLE_SESSION_NAME_LENGTH = 64;
    private final StsClient stsClient;
    private final String roleArn;
    private final String agentId;

    /**
     * Generates temporary AWS credentials for an execution.
     */
    public AwsSessionCredentials generateCredentialsForExecution(Execution execution) {
        var sessionPolicy = createPolicyForExecution(execution);
        var response = stsClient.assumeRole(
                r -> r.roleArn(roleArn)
                        .roleSessionName(generateRoleSessionName(execution.getUsername()))
                        .policy(sessionPolicy.toJson())
                        .durationSeconds(TOKEN_LIFETIME)
                        .externalId(execution.getProjectId())
        );
        return AwsSessionCredentials.builder()
                .accessKeyId(response.credentials().accessKeyId())
                .secretAccessKey(response.credentials().secretAccessKey())
                .sessionToken(response.credentials().sessionToken())
                .expirationTime(response.credentials().expiration())
                .build();
    }

    /**
     * Generates a role session name for the execution, limited to the maximum length.
     * @implNote Role session name is used to capture information about who is assuming the role.
     * This is saved in CloudTrail logs.
     */
    private String generateRoleSessionName(String username) {
        var roleSessionName = String.format("%s-%s", agentId, username);
        return roleSessionName.substring(0, Math.min(roleSessionName.length(), MAX_ROLE_SESSION_NAME_LENGTH));
    }

    /**
     * Creates a policy that restricts the agent to only the dataset and project of the execution.
     */
    private IamPolicy createPolicyForExecution(Execution execution) {
        var datasetS3Path = execution.getDatasetS3Path();
        var bucketArn = Arn.builder()
                .partition("aws")
                .service(S3Client.SERVICE_NAME)
                .resource(datasetS3Path.bucket())
                .build()
                .toString();

        return IamPolicy.builder()
                // General S3 permissions
                .addStatement(b -> b
                        .sid("AllowListBucket")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:ListBucket")
                        .addAction("s3:GetBucketLocation")
                        .addResource("*")
                )
                // Restrict the agent to only write to the current dataset's path
                .addStatement(b -> b
                        .sid("AllowWriteToDataset")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:PutObject")
                        .addAction("s3:DeleteObject")
                        .addResource(String.format("%s/%s/*", bucketArn, datasetS3Path.key()))
                )
                // The agent role that is assumed by the agent is already scoped to the project bucket,
                // as well as any cross-account access that it was granted,
                // so we don't need to add any additional restrictions here
                .addStatement(b -> b
                        .sid("AllowRead")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:GetObject*")
                        .addResource("*")
                )
                // Same with this, the agent role is already scoped to the project's KMS key
                .addStatement(b -> b
                        .sid("AllowKMS")
                        .effect(IamEffect.ALLOW)
                        .addAction("kms:Decrypt")
                        .addAction("kms:GenerateDataKey*")
                        .addResource("*")
                )
                // Agent should be allowed to pull any image from ECR
                .addStatement(b -> b
                        .sid("AllowPullImage")
                        .effect(IamEffect.ALLOW)
                        .addAction("ecr:GetAuthorizationToken")
                        .addAction("ecr:BatchCheckLayerAvailability")
                        .addAction("ecr:GetDownloadUrlForLayer")
                        .addAction("ecr:BatchGetImage")
                        .addResource("*")
                )
                .build();
    }
}
