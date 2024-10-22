package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.AgentTokenService;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.models.Status;
import bio.cirro.agent.utils.FileUtils;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionCreateService {
    private static final Pattern JOB_ID_REGEX = Pattern.compile("^\\d+$");

    private final AgentConfig agentConfig;
    private final AgentTokenService agentTokenService;
    private final ExecutionRepository executionRepository;

    public ExecutionSession createSession(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var sessionId = UUID.randomUUID().toString();
        var workingDirectory = Paths.get(agentConfig.getAbsoluteWorkDirectory().toString(), sessionId);

        var session = ExecutionSession.builder()
                .sessionId(sessionId)
                .messageData(runAnalysisCommandMessage)
                .workingDirectory(workingDirectory)
                .status(Status.PENDING)
                .createdAt(Instant.now())
                .build();
        executionRepository.add(session);

        var token = agentTokenService.generateForSession(sessionId);
        try {
            // Set up working directory
            Files.createDirectories(workingDirectory);
            writeEnvironment(session, token);
            writeAwsConfig(session);

            var executionOutput = startExecution(session);
            session.setOutput(executionOutput);
        } catch (Exception ex) {
            executionRepository.removeSession(sessionId);
            throw new ExecutionException("Failed to start execution", ex);
        }
        return session;
    }

    private void writeEnvironment(ExecutionSession session, String token) {
        var environmentVariables = session.getEnvironment();
        environmentVariables.put("CIRRO_TOKEN", token);
        environmentVariables.put("CIRRO_AGENT_ENDPOINT", "http://localhost:8080");

        var environmentSb = new StringBuilder();
        environmentSb.append("#!/bin/bash\n");
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

    private void writeAwsConfig(ExecutionSession session) {
        // Write AWS config file
        var awsConfigTemplate = FileUtils.getResourceAsString("aws-config.properties");
        awsConfigTemplate = awsConfigTemplate.replace("%%SESSION_ID%%", session.getSessionId());
        awsConfigTemplate = awsConfigTemplate.replace("%%AGENT_URL%%", "");

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
            env.put("WORKING_DIR", session.getWorkingDirectory().toString());

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
            return new ExecutionSessionOutput(processOutput, jobId);
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
    }
}
