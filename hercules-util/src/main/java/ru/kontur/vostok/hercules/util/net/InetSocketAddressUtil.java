package ru.kontur.vostok.hercules.util.net;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.net.InetSocketAddress;

/**
 * @author Gregory Koshelev
 */
public final class InetSocketAddressUtil {
    /**
     * Parse {@link InetSocketAddress} from {@code <host>[:<port>]} string.
     * <p>
     * The {@code port} is optional. Thus, {@code defaultPort} is used if {@code hostAndPort} string contains only the {@code host}.
     *
     * @param hostAndPort    {@code <host>[:<port>]} string
     * @param defaultPort default port
     * @return parsed {@link InetSocketAddress}
     * @throws IllegalArgumentException is something goes wrong
     */
    public static InetSocketAddress fromString(@NotNull String hostAndPort, int defaultPort) {
        String[] parts = StringUtil.requireNotEmpty(hostAndPort).split(":");
        if (parts.length == 1) {
            return new InetSocketAddress(parts[0], checkPort(defaultPort));
        }
        if (parts.length == 2) {
            return new InetSocketAddress(parts[0], checkPort(parsePort(parts[1])));
        }
        throw new IllegalArgumentException("HostPort string has invalid format: " + hostAndPort);
    }

    /**
     * Check if {@code port} is valid port number ({@code 0 <= port < 0xFFFF}).
     *
     * @param port port number
     * @return valid port number
     * @throws IllegalArgumentException if {@code port} out of range
     */
    public static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range: " + port);
        }
        return port;
    }

    /**
     * Parse {@code port} string and returns valid port number if possible, or throw {@link IllegalArgumentException} otherwise.
     *
     * @param port port number
     * @return valid port number
     * @throws NumberFormatException if {@code port} is not an integer
     * @throws IllegalArgumentException if {@code port} out of range
     */
    public static int parsePort(String port) {
        return checkPort(Integer.parseInt(port));
    }

    private InetSocketAddressUtil() {
        /* static class */
    }
}
