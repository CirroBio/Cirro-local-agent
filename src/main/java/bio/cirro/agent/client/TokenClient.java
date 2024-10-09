package bio.cirro.agent.client;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.execution.ExecutionSession;
import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

import java.time.Duration;
import java.util.Optional;

@Singleton
@AllArgsConstructor
public class TokenClient {
    private static final int TOKEN_LIFETIME = (int) Duration.ofHours(1).getSeconds();
    private final StsClient stsClient;
    private final String roleArn;
    private final String agentId;

    @Bean
    public static TokenClient create(AgentConfig agentConfig) {
        return new TokenClient(
                StsClient.create(),
                agentConfig.getFileAccessRoleArn(),
                agentConfig.getId()
        );
    }

    public String getCurrentIdentity() {
        return stsClient.getCallerIdentity().arn();
    }

    public AwsSessionCredentials generateCredentialsForExecutionSession(ExecutionSession executionSession) {
        var roleSessionName = String.format("%s-%s", agentId, executionSession.getUsername());
        var sessionPolicy = createPolicyForExecutionSession(executionSession);
        var response = stsClient.assumeRole(
                r -> r.roleArn(roleArn)
                        .roleSessionName(roleSessionName)
                        .policy(sessionPolicy.toJson())
                        .durationSeconds(TOKEN_LIFETIME)
                        .externalId(agentId)
        );
        return AwsSessionCredentials.builder()
                .accessKeyId(response.credentials().accessKeyId())
                .secretAccessKey(response.credentials().secretAccessKey())
                .sessionToken(response.credentials().sessionToken())
                .expirationTime(response.credentials().expiration())
                .build();
    }

    private IamPolicy createPolicyForExecutionSession(ExecutionSession executionSession) {
        var datasetS3Path = executionSession.getDatasetS3Path();
        var bucketArn = Arn.builder()
                .partition(Optional.ofNullable(executionSession.getProjectAccount().partition()).orElse("aws"))
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
                        .addResource(bucketArn)
                )
                .addStatement(b -> b
                        .sid("AllowWriteToDataset")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:PutObject")
                        .addAction("s3:DeleteObject")
                        .addResource(String.format("%s/%s/*", bucketArn, datasetS3Path.getKey()))
                )
                .addStatement(b -> b
                        .sid("AllowReadFromProject")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:GetObject*")
                        .addResource(String.format("%s/*", bucketArn))
                )
                .addStatement(b -> b
                        .sid("AllowReadCrossAccount")
                        .effect(IamEffect.ALLOW)
                        .addAction("s3:GetObject*")
                        .addAction("s3:PutObject")
                        .addAction("s3:ListBucket")
                        .addAction("s3:GetBucketLocation")
                        .addResource("*")
                        .addCondition(c -> c
                                .operator(IamConditionOperator.STRING_NOT_EQUALS)
                                .key("aws:ResourceAccount")
                                .value(executionSession.getProjectAccount().accountId())
                        )
                )
                .build();
    }
}
