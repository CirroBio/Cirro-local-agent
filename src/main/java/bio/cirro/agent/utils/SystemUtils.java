package bio.cirro.agent.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SystemUtils {
    public static String getOs() {
        return String.format("%s %s", System.getProperty("os.name"), System.getProperty("os.version"));
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
}
