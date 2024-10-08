package bio.cirro.agent.utils;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.execution.ExecutionSession;
import bio.cirro.agent.models.AWSCredentials;
import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.arns.Arn;
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

    public AWSCredentials generateCredentialsForExecutionSession(ExecutionSession executionSession) {
        var roleSessionName = String.format("%s-%s", agentId, executionSession.username());
        var sessionPolicy = createPolicyForExecutionSession(executionSession);
        var token = stsClient.assumeRole(
                r -> r.roleArn(roleArn)
                        .roleSessionName(roleSessionName)
                        .policy(sessionPolicy.toJson())
                        .durationSeconds(TOKEN_LIFETIME)
                        .externalId(agentId)
        );

        return AWSCredentials.builder()
                .accessKeyId(token.credentials().accessKeyId())
                .secretAccessKey(token.credentials().secretAccessKey())
                .sessionToken(token.credentials().sessionToken())
                .expiration(token.credentials().expiration())
                .build();
    }

    private IamPolicy createPolicyForExecutionSession(ExecutionSession executionSession) {
        var datasetS3Path = executionSession.datasetS3Path();
        var bucketArn = Arn.builder()
                .partition(Optional.ofNullable(executionSession.projectAccount().partition()).orElse("aws"))
                .service(S3Client.SERVICE_NAME)
                .resource(datasetS3Path.getBucket())
                .build()
                .toString();

        return IamPolicy.builder()
                .addStatement(b -> b
                        .sid("AllowListBucket")
                        .effect("Allow")
                        .addAction("s3:ListBucket")
                        .addAction("s3:GetBucketLocation")
                        .addResource(bucketArn)
                )
                .addStatement(b -> b
                        .sid("AllowWriteToDataset")
                        .effect("Allow")
                        .addAction("s3:PutObject")
                        .addAction("s3:DeleteObject")
                        .addResource(String.format("%s/%s/*", bucketArn, datasetS3Path.getKey()))
                )
                .addStatement(b -> b
                        .sid("AllowReadFromProject")
                        .effect("Allow")
                        .addAction("s3:GetObject*")
                        .addResource(String.format("%s/*", bucketArn))
                )
                .addStatement(b -> b
                        .sid("AllowReadCrossAccount")
                        .effect("Allow")
                        .addAction("s3:GetObject*")
                        .addAction("s3:PutObject")
                        .addAction("s3:ListBucket")
                        .addAction("s3:GetBucketLocation")
                        .addResource("*")
                        .addCondition(c -> c
                                .operator("StringNotEquals")
                                .key("aws:ResourceAccount")
                                .value(executionSession.projectAccount().accountId())
                        )
                )
                .build();
    }
}
