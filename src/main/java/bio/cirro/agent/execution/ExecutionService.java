package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.client.TokenClient;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.models.AWSCredentials;
import bio.cirro.agent.utils.FileUtils;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import software.amazon.awssdk.services.sts.StsClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionService {
    private static final Pattern JOB_ID_REGEX = Pattern.compile("^\\d+$");
    private static final List<String> ALLOWED_ENV_PREFIXES = List.of("PW_", "CIRRO_");

    private final AgentConfig agentConfig;
    private final ExecutionRepository executionRepository;
    private final StsClient stsClient;

    public ExecutionSession createSession(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var sessionId = UUID.randomUUID().toString();
        var workingDirectory = Paths.get(agentConfig.getAbsoluteWorkDirectory().toString(), sessionId);
        var session = ExecutionSession.builder()
                .sessionId(sessionId)
                .messageData(runAnalysisCommandMessage)
                .workingDirectory(workingDirectory)
                .build();
        executionRepository.add(session);

        var tokenClient = new TokenClient(stsClient, session.getFileAccessRoleArn(), agentConfig.getId());
        var creds = tokenClient.generateCredentialsForExecutionSession(session);
        session.setAwsCredentials(creds);

        try {
            // Set up working directory
            writeEnvironment(session);
            writeAwsConfig(session);

            var executionOutput = startExecution(session);
            session.setOutput(executionOutput);
        } catch (Exception ex) {
            executionRepository.removeSession(sessionId);
            throw new ExecutionException("Failed to start execution", ex);
        }
        return session;
    }

    public AWSCredentials generateExecutionS3Credentials(String sessionId) {
        var session = executionRepository.getSession(sessionId);
        var tokenClient = new TokenClient(stsClient, session.getFileAccessRoleArn(), agentConfig.getId());
        var creds = tokenClient.generateCredentialsForExecutionSession(session);
        return AWSCredentials.builder()
                .accessKeyId(creds.accessKeyId())
                .secretAccessKey(creds.secretAccessKey())
                .sessionToken(creds.sessionToken())
                .expiration(creds.expirationTime().orElse(null))
                .build();
    }

    public void completeExecution(String sessionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.removeSession(sessionId);
    }

    private void writeEnvironment(ExecutionSession session) {
        var environmentVariables = generateEnvironment(session);

        var environmentSb = new StringBuilder();
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            environmentSb.append(String.format("export %s=%s%n", entry.getKey(), entry.getValue()));
        }

        var environmentFile = session.getEnvironmentPath();
        try {
            FileUtils.writeScript(environmentFile, environmentSb.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write environment file", e);
        }
    }

    private Map<String, String> generateEnvironment(ExecutionSession session) {
        var environment = new HashMap<String, String>();
        environment.put("PW_DATASET", session.getDatasetId());
        environment.put("AWS_CONFIG_FILE", session.getAwsConfigPath().toString());
        environment.put("AWS_SHARED_CREDENTIALS_FILE", session.getAwsCredentialsPath().toString());

        // Add any variables injected by Cirro
        for (var variable : session.getMessageData().getEnvironment().entrySet()) {
            // Check if variable is allowed to be set
            if (ALLOWED_ENV_PREFIXES.stream().noneMatch(variable.getKey()::startsWith)) {
                log.warn("Setting of environment variable {} not allowed", variable.getKey());
                continue;
            }

            environment.put(variable.getKey(), StringEscapeUtils.escapeXSI(variable.getValue()));
        }

        return environment;
    }

    private void writeAwsConfig(ExecutionSession session) {
        // Write AWS config file
        var awsConfigTemplate = FileUtils.getResourceAsString("aws-config.properties");
        awsConfigTemplate = awsConfigTemplate.replace("%%SESSION_ID%%", session.getSessionId());

        try {
            Files.writeString(session.getAwsConfigPath(), awsConfigTemplate);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write AWS config", e);
        }

        // Write credential helper
        var credentialHelperScript = FileUtils.getResourceAsString("credential-helper.sh");
        try {
            FileUtils.writeScript(session.getCredentialsHelperPath(), credentialHelperScript);
        } catch (IOException e) {
            throw new ExecutionException("Failed to write credential helper", e);
        }
    }

    private ExecutionSessionOutput startExecution(ExecutionSession session) {
        try {
            // Headnode launch script
            // pass working directory, user-provided

            // Headnode execution script
            // Load "Cirro" headnode script
            // Load user-defined script
            // str.replace("## TEMPLATE", user-defined script)
            // Write to working directory

            Path launchScript = Paths.get(agentConfig.getAbsoluteScriptsDirectory().toString(), "submit_headnode.sh");
            if (!launchScript.toFile().exists()) {
                throw new ExecutionException("Launch script not found", null);
                // TODO: Write default launch script?
            }
            log.debug("Using launch script: {}", launchScript);
            var headnodeLaunchProcessBuilder = new ProcessBuilder()
                    .directory(session.getWorkingDirectory().toFile())
                    .command("sh", launchScript.toAbsolutePath().toString())
                    .redirectErrorStream(true);
            // Set environment variables
            var env = headnodeLaunchProcessBuilder.environment();
            env.put("PROCESS_DIR", session.getWorkingDirectory().toString());
            env.put("PROCESS_NAME", session.getDatasetId());
            env.putAll(generateEnvironment(session));

            var headnodeLaunchProcess = headnodeLaunchProcessBuilder.start();

            StringBuilder processOutputSb = new StringBuilder();
            String jobId = null;
            try (var reader = new BufferedReader(new InputStreamReader(headnodeLaunchProcess.getInputStream()))) {
                for (String line : reader.lines().toList()) {
                    // Try to extract job ID
                    if (JOB_ID_REGEX.matcher(line).matches()) {
                        jobId = line;
                    }
                    processOutputSb.append(line).append("\n");
                }
            }
            var processOutput = processOutputSb.toString();
            log.debug("Execution output: {}", processOutput);
            if (jobId == null) {
                throw new ExecutionException("Failed to extract job ID");
            }

            headnodeLaunchProcess.waitFor(10, TimeUnit.SECONDS);

            if (headnodeLaunchProcess.exitValue() != 0) {
                throw new ExecutionException("Execution failed: " + processOutput);
            }
            headnodeLaunchProcess.destroy();
            return new ExecutionSessionOutput(jobId, processOutput);
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
    }
}
