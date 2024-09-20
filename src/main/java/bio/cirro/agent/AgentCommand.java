package bio.cirro.agent;

import bio.cirro.agent.dto.AgentRegisterMessage;
import bio.cirro.agent.dto.HeartbeatMessage;
import bio.cirro.agent.exception.AgentException;
import bio.cirro.agent.socket.AgentClient;
import bio.cirro.agent.socket.AgentClientFactory;
import bio.cirro.agent.socket.ConnectionInfo;
import ch.qos.logback.classic.Level;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;

/**
 * Cirro Agent entry point.
 */
@Command(
        name = "cirro-agent",
        mixinStandardHelpOptions = true
)
@Singleton
@Slf4j
@RequiredArgsConstructor
public class AgentCommand implements Runnable {
    // Injected dependencies
    private final AgentClientFactory agentClientFactory;
    private final TaskScheduler taskScheduler;
    private final MessageHandler messageHandler;

    // Command line options
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

    // Internal state
    private AgentClient clientSocket;

    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    /**
     * Set up the Application Context and command line.
     * <p>
     * This is not technically required as one can just use PicocliRunner
     * in the main method, but this provides additional customization options.
     */
    private static int execute(String[] args) {
        try (ApplicationContext context = ApplicationContext.builder(AgentCommand.class, Environment.CLI).start()) {
            CommandLine cmd = new CommandLine(AgentCommand.class, new MicronautFactory(context)).
                    setCaseInsensitiveEnumValuesAllowed(true).
                    setUsageHelpAutoWidth(true);
            // Print header with ANSI colors on all cases (not just help text)
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

    /**
     * Main agent loop
     * Gets called once the CLI framework is done (i.e., after parsing the command line arguments).
     */
    @Override
    public void run() {
        try {
            if (verbose) {
                enableVerboseLogging();
            }
            validateParams();

            // Schedule connection watcher and heartbeat tasks
            var watcher = taskScheduler.scheduleAtFixedRate(Duration.ZERO, Duration.ofSeconds(2), this::watchAndInitConnection);
            taskScheduler.scheduleAtFixedRate(Duration.ofSeconds(heartbeatInterval), Duration.ofSeconds(heartbeatInterval), this::sendHeartbeat);

            // Wait for the watcher task to complete (it only completes when an exception is thrown)
            watcher.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted", e);
            System.exit(1);
        } catch (AgentException e) {
            log.error("Error: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            System.exit(1);
        }
    }

    /**
     * Initialize the connection to the server if it is not already open
     */
    private void watchAndInitConnection() {
        try {
            if (clientSocket == null || !clientSocket.isOpen()) {
                var connectionInfo = ConnectionInfo.builder()
                        .url(url)
                        .token(token)
                        .build();
                clientSocket = agentClientFactory.connect(connectionInfo, messageHandler::handleMessage);
                clientSocket.sendMessage(new AgentRegisterMessage(agentId));
            }
        } catch (WebSocketClientException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Send a heartbeat message to the server to avoid disconnection
     */
    private void sendHeartbeat() {
        if (clientSocket != null && clientSocket.isOpen()) {
            clientSocket.sendMessage(new HeartbeatMessage());
        }
    }

    /**
     * Validate required parameters
     */
    private void validateParams() {
        if (url == null || url.isBlank()) {
            throw new AgentException("URL is required");
        }
    }

    /**
     * Sets log level to DEBUG for the {@link bio.cirro.agent} package
     */
    private void enableVerboseLogging() {
        log.info("Enabling verbose logging");
        var agentLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AgentCommand.class.getPackageName());
        agentLogger.setLevel(Level.DEBUG);
    }
}
