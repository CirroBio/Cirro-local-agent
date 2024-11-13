package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.aws.AwsTokenClient;
import bio.cirro.agent.aws.S3Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ExecutionTokenServiceTest {
    private static final String MOCK_ID = "test";
    ExecutionTokenService executionTokenService;
    Execution mockExecution;

    @BeforeEach
    void setUp() {
        var executionRepository = mock(ExecutionRepository.class);
        var stsClient = mock(StsClient.class);
        var agentConfig = mock(AgentConfig.class);
        mockExecution = mock(Execution.class);
        doReturn(MOCK_ID).when(mockExecution).getDatasetId();
        doReturn(mockExecution).when(executionRepository).get(MOCK_ID);
        doReturn(S3Path.parse("s3://project/dataset")).when(mockExecution).getDatasetS3Path();
        executionTokenService = spy(new ExecutionTokenService(executionRepository, stsClient, agentConfig));
        var tokenClient = mock(AwsTokenClient.class);
        doReturn(tokenClient).when(executionTokenService).createTokenClient(any());
        doReturn(mock(AwsSessionCredentials.class)).when(tokenClient).generateCredentialsForExecution(any());
    }

    @Test
    void testGenerateS3Credentials_happyPath() {
        var creds = executionTokenService.generateS3Credentials(MOCK_ID);
        Assertions.assertNotNull(creds);
    }

    @Test
    void testGenerateS3Credentials_withinThreshold() {
        doReturn(Instant.now().minus(Duration.ofSeconds(10))).when(mockExecution).getFinishedAt();
        var creds = executionTokenService.generateS3Credentials(MOCK_ID);
        Assertions.assertNotNull(creds);
    }

    @Test
    void testGenerateS3CredentialsCompleted_throwsError() {
        doReturn(Instant.now().minus(Duration.ofHours(1))).when(mockExecution).getFinishedAt();
        Assertions.assertThrows(RuntimeException.class,
                () -> executionTokenService.generateS3Credentials(MOCK_ID));
    }
}
