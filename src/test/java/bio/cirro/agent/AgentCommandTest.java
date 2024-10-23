package bio.cirro.agent;

import bio.cirro.agent.messaging.AgentClientFactory;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.TaskScheduler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AgentCommandTest {

    @Test
    void testAgentCommand() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            PicocliRunner.run(AgentCommand.class, ctx, "--help");
            assertTrue(baos.toString().contains("Usage: cirro-agent"));
        }
    }

    @Test
    @Disabled("Fix me")
    void testDebugOption() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        AgentClientFactory mockAgentClientFactory = mock(AgentClientFactory.class);
        TaskScheduler mockTaskScheduler = mock(TaskScheduler.class);
        doReturn(mock(ScheduledFuture.class)).when(mockTaskScheduler).scheduleAtFixedRate(any(), any(), any());

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            ctx.registerSingleton(AgentClientFactory.class, mockAgentClientFactory);
            ctx.registerSingleton(TaskScheduler.class, mockTaskScheduler);
            PicocliRunner.run(AgentCommand.class, ctx, "--debug");
            assertTrue(baos.toString().contains("DEBUG"));
        }
    }
}
