package bio.cirro.agent.utils;

import bio.cirro.agent.exception.AgentException;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

public class FileUtils {
    private FileUtils() {
        // Utility class
    }
    public static void writeScript(Path scriptPath, String script) throws IOException {
        Files.writeString(scriptPath, script);
        var permissions = Files.getPosixFilePermissions(scriptPath);
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        Files.setPosixFilePermissions(scriptPath, permissions);
    }

    @SneakyThrows(IOException.class)
    public static String getResourceAsString(String resourcePath) {
        try (var stream = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assert stream != null;
            return new String(stream.readAllBytes());
        }
    }

    public static void validateDirectory(Path directory, String label) {
        try {
            if (!Files.isDirectory(directory)) {
                throw new AgentException(String.format("%s directory does not exist: %s", label, directory));
            }
        } catch (InvalidPathException e) {
            throw new AgentException(String.format("%s directory is invalid: %s", label, directory));
        }
    }
}
