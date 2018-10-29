package ru.kontur.vostok.hercules.util.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * LocalhostResolver
 *
 * @author Kirill Sulim
 */
public final class LocalhostResolver {

    public static String getLocalHostName() {
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostName();
            if (Objects.nonNull(host) && !host.isEmpty()) {
                return host;
            }
        } catch (UnknownHostException e) {
            /* use alternate ways */
        }

        host = System.getenv("COMPUTERNAME");
        if (Objects.nonNull(host)) {
            return host;
        }
        host = System.getenv("HOSTNAME");
        if (Objects.nonNull(host)) {
            return host;
        }

        return null;
    }

    private LocalhostResolver() {
    }
}
