package bio.cirro.agent.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;

public class SystemUtils {

    private SystemUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getOs() {
        return String.format("%s %s %s", System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
    }

    public static String getJavaVersion() {
        return String.format("%s (%s %s)", System.getProperty("java.runtime.name"), System.getProperty("java.vendor"), System.getProperty("java.vendor.version"));
    }

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
