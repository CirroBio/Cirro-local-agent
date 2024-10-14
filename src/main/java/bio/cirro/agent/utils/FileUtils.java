package bio.cirro.agent.utils;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
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
}
