package bio.cirro.agent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class AgentTokenServiceTest {
    private static final String MOCK_AGENT_ID = "test-agent";
    AgentTokenService agentTokenService;

    @BeforeEach
    void setUp() {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setJwtSecret("secret".getBytes());
        agentConfig.setId(MOCK_AGENT_ID);
        agentConfig.setJwtExpiryDays(1);
        agentTokenService = new AgentTokenService(agentConfig);
    }

    @Test
    void testSign() {
        var mockExecutionId = UUID.randomUUID().toString();

        var result = agentTokenService.generateForExecution(mockExecutionId);
        var payloadBase64 = result.split("\\.")[1];
        var payload = new String(java.util.Base64.getDecoder().decode(payloadBase64));
        Assertions.assertTrue(payload.contains(mockExecutionId));
        Assertions.assertTrue(payload.contains(MOCK_AGENT_ID));
        Assertions.assertDoesNotThrow(() -> agentTokenService.validate(result, mockExecutionId));
    }

    @Test
    void testVerifyBadExecutionId() {
        var mockExecutionId = UUID.randomUUID().toString();
        var differentId = UUID.randomUUID().toString();
        var token = agentTokenService.generateForExecution(mockExecutionId);
        Assertions.assertThrows(SecurityException.class,
                () -> agentTokenService.validate(token, differentId));
    }

    @Test
    void testVerifyBadToken() {
        Assertions.assertThrows(SecurityException.class,
                () -> agentTokenService.validate("bad-token", "1"));
    }
}
