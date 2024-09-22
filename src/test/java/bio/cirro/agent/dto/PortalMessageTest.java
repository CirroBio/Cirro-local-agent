package bio.cirro.agent.dto;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

class PortalMessageTest {

    @ParameterizedTest
    @CsvSource(value = {
        "{};UnknownMessage",
        "{\"agentId\":\"test\", \"type\":\"register\"};AgentRegisterMessage",
        "{\"type\":\"ack\"};AckMessage",
    }, delimiter = ';')
    void testMapping(String json, String expectedType) throws IOException {
        var objectMapper = ObjectMapper.getDefault();
        var message = objectMapper.readValue(json, PortalMessage.class);
        var actualType = message.getClass().getSimpleName();
        Assertions.assertEquals(expectedType, actualType);
    }
}
