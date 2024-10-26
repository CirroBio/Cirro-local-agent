package bio.cirro.agent.utils;

import bio.cirro.agent.exception.AgentException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

@Slf4j
public class FileUtils {
    private FileUtils() {
        // Utility class
    }

    /**
     * Writes a script to a file and sets the file permissions to be executable.
     * @param scriptPath Path to write the script to
     * @param script Script content
     * @throws IOException if an I/O error occurs
     */
    public static void writeScript(Path scriptPath, String script) throws IOException {
        Files.writeString(scriptPath, script);
        try {
            var permissions = Files.getPosixFilePermissions(scriptPath);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            Files.setPosixFilePermissions(scriptPath, permissions);
        } catch (UnsupportedOperationException e) {
            log.debug("Cannot set script permissions on non-POSIX file system: {}", scriptPath);
        }
    }

    /**
     * Reads a resource file as a string.
     * @param resourcePath Path to the resource file
     * @return Resource file content as a string
     */
    @SneakyThrows(IOException.class)
    public static String getResourceAsString(String resourcePath) {
        try (var stream = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assert stream != null;
            return new String(stream.readAllBytes());
        }
    }

    /**
     * Validates that a directory exists.
     * @param directory Directory to validate
     * @param label Label for the directory (used in error messages)
     */
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
