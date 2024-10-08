package bio.cirro.agent.execution;

import bio.cirro.agent.AgentConfig;
import bio.cirro.agent.dto.RunAnalysisCommandMessage;
import bio.cirro.agent.exception.ExecutionException;
import bio.cirro.agent.models.AWSCredentials;
import bio.cirro.agent.utils.TokenClient;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@AllArgsConstructor
@Slf4j
public class ExecutionService {
    private final AgentConfig agentConfig;
    private final TokenClient tokenClient;
    private final ExecutionRepository executionRepository;

    public ExecutionSession createSession(RunAnalysisCommandMessage runAnalysisCommandMessage) {
        var sessionId = UUID.randomUUID().toString();
        var session = ExecutionSession.builder()
                .sessionId(sessionId)
                .datasetId(runAnalysisCommandMessage.getDatasetId())
                .projectId(runAnalysisCommandMessage.getProjectId())
                .datasetS3(runAnalysisCommandMessage.getDatasetPath())
                .build();
        executionRepository.add(session);
        return session;
    }

    public AWSCredentials generateExecutionS3Credentials(String sessionId) {
        var session = executionRepository.getSession(sessionId);
        return tokenClient.generateCredentialsForExecutionSession(session);
    }

    public void completeExecution(String sessionId) {
        // Handle stuff
        // Remove if everything is successful
        executionRepository.removeSession(sessionId);
    }

    private void writeParams(ExecutionSession session) {

    }

    private void writeConfig(ExecutionSession session) {
        // Write nextflow config file.
        // What should we put here vs the headnode script?
    }

    private void writeEnvironment(ExecutionSession session) {
        Map<String, String> environmentVariables = Map.ofEntries(
                Map.entry("DATASET_ID", session.getDatasetId())
        );

        StringBuilder environmentFileString = new StringBuilder();
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            environmentFileString.append(String.format("export %s=%s%n", entry.getKey(), entry.getValue()));
        }

        var environmentFilePath = Paths.get(session.getWorkingDirectory().toString(), "environment.sh");
        try {
            Files.writeString(environmentFilePath, environmentFileString.toString());
        } catch (IOException e) {
            throw new ExecutionException("Failed to write environment file", e);
        }
    }

    private void writeAWSConfig(ExecutionSession session) {
        // Write AWS config file
        // Write credentials-helper.sh
    }

    private String startExecution(ExecutionSession session) {
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
            var process = new ProcessBuilder()
                    .directory(session.getWorkingDirectory().toFile())
                    .command("sh", launchScript.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .start();

            var output = new StringBuilder();
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor(10, TimeUnit.SECONDS);

            var processOutput = output.toString();

            if (process.exitValue() != 0) {
                throw new ExecutionException("Execution failed: " + processOutput);
            }
            log.debug("Execution output: {}", processOutput);
            return processOutput;
        } catch (IOException e) {
            throw new ExecutionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Thread interrupted", e);
        }
    }
}
