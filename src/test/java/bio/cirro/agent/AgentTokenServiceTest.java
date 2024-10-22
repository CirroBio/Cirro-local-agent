package bio.cirro.agent;

import com.auth0.jwt.exceptions.JWTVerificationException;
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
        agentConfig.setJwtSecret("secret");
        agentConfig.setId(MOCK_AGENT_ID);
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
        var executionId = agentTokenService.validate(result);
        Assertions.assertEquals(mockExecutionId, executionId);
    }

    @Test
    void testVerifyBadToken() {
        Assertions.assertThrows(JWTVerificationException.class,
                () -> agentTokenService.validate("bad-token"));
    }
}
