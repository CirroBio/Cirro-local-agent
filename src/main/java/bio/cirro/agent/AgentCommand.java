package bio.cirro.agent;

import bio.cirro.agent.dto.AgentRegisterMessage;
import bio.cirro.agent.dto.HeartbeatMessage;
import bio.cirro.agent.socket.AgentClient;
import bio.cirro.agent.socket.AgentClientFactory;
import bio.cirro.agent.socket.ConnectionInfo;
import ch.qos.logback.classic.Level;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.TaskScheduler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Cirro Agent entry point.
 */
@Command(
        name = "cirro-agent",
        mixinStandardHelpOptions = true
)
@Singleton
@Slf4j
public class AgentCommand implements Runnable {

    @Option(names = {"-i", "--id"}, description = "Agent ID", defaultValue = "${env:AGENT_ID}", required = true)
    String agentId;

    @Option(names = {"-h", "--heartbeat-interval"}, description = "Heartbeat interval in seconds", defaultValue = "${env:AGENT_HEARTBEAT_INTERVAL:-60}", required = true)
    int heartbeatInterval;

    @Option(names = {"-url", "--url"}, description = "Server URL", defaultValue = "${env:AGENT_SERVER_URL}", required = true)
    String url;

    @Option(names = {"-t", "--token"}, description = "Agent token", defaultValue = "${env:AGENT_TOKEN}", required = true)
    String token;

    @Option(names = {"-d", "--debug"}, description = "Enable debug logging", defaultValue = "${env:AGENT_DEBUG:-false}")
    boolean verbose;

    AgentClient clientSocket;

    @Inject
    AgentClientFactory agentClientFactory;

    @Inject
    TaskScheduler taskScheduler;

    @Inject
    MessageHandler messageHandler;

    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    private static int execute(String[] args) {
        try (ApplicationContext context = ApplicationContext.builder(AgentCommand.class, Environment.CLI).start()) {
            CommandLine cmd = new CommandLine(AgentCommand.class, new MicronautFactory(context)).
                    setCaseInsensitiveEnumValuesAllowed(true).
                    setUsageHelpAutoWidth(true);
            var header = new String[]{
                    "@|green  __     __   __   __ |@",
                    "@|green /  ` | |__) |__) /  \\|@",
                    "@|green \\__, | |  \\ |  \\ \\__/|@",
                    "@|green                       |@",
                    "@|yellow Cirro Agent version 0.1.0 |@"
            };
            for (String line : header) {
                System.out.println(CommandLine.Help.Ansi.AUTO.string(line));
            }
            return cmd.execute(args);
        }
    }

    @Override
    public void run() {
        try {
            if (verbose) {
                enableVerboseLogging();
            }
            validateParams();

            // Schedule connection and heartbeat tasks
            taskScheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofSeconds(2), this::watchConnection);
            taskScheduler.scheduleAtFixedRate(Duration.ofSeconds(heartbeatInterval), Duration.ofSeconds(heartbeatInterval), this::sendHeartbeat);

            // Wait forever
            CountDownLatch latch = new CountDownLatch(1);
                latch.await();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Error", e);
            System.exit(1);
        }
    }

    // Setup connection to server and reconnect if disconnected
    private void watchConnection() {
        try {
            if (clientSocket == null || !clientSocket.isOpen()) {
                var connectionInfo = ConnectionInfo.builder()
                        .url(url)
                        .token(token)
                        .build();
                clientSocket = agentClientFactory.connect(connectionInfo, messageHandler::handleMessage);
                clientSocket.sendMessage(new AgentRegisterMessage(agentId));
            }
        } catch (Exception e) {
            log.error("Error connecting to server", e);
            throw new RuntimeException(e);
        }
    }

    // Send heartbeat to keep connection alive
    private void sendHeartbeat() {
        if (clientSocket != null && clientSocket.isOpen()) {
            clientSocket.sendMessage(new HeartbeatMessage());
        }
    }

    // Validate required parameters
    private void validateParams() {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
    }

    private void enableVerboseLogging() {
        log.info("Enabling verbose logging");
        var agentLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AgentCommand.class.getPackageName());
        agentLogger.setLevel(Level.DEBUG);
    }
}
