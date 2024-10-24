package bio.cirro.agent.execution;

import bio.cirro.agent.messaging.dto.RunAnalysisCommandMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

class ExecutionTest {

    @Test
    void testExecution() {
        var execution = makeExecution(Map.of("key", "value"));
        var messageData = execution.getMessageData();
        Assertions.assertEquals(execution.getExecutionId(), messageData.getDatasetId());
        Assertions.assertEquals(execution.getDatasetId(), messageData.getDatasetId());
        Assertions.assertEquals(execution.getProjectId(), messageData.getProjectId());

        var environment = execution.getEnvironment("token", "agentEndpoint");
        Assertions.assertFalse(environment.isEmpty());
    }

    @Test
    void testEnvironmentShouldEscape() {
        var execution = makeExecution(Map.of("INJECT", "\"exit"));
        var env = execution.getEnvironment("token", "agentEndpoint");
        Assertions.assertEquals("\\\"exit", env.get("INJECT"));
    }

    private Execution makeExecution(Map<String, String> environment) {
        var messageData = RunAnalysisCommandMessage.builder()
                .datasetId("123")
                .projectId("456")
                .username("username")
                .datasetPath("datasetPath")
                .fileAccessRoleArn("fileAccessRoleArn")
                .region("region")
                .environment(environment)
                .build();
        Path testDir = Path.of(".");
        return Execution.builder()
                .messageData(messageData)
                .agentWorkingDirectory(testDir)
                .agentSharedDirectory(testDir)
                .build();
    }
}
