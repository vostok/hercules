package ru.kontur.vostok.hercules.util.net;

import ru.kontur.vostok.hercules.util.EnvironmentUtil;
import ru.kontur.vostok.hercules.util.ObjectUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * LocalhostResolver
 *
 * @author Kirill Sulim
 */
public final class LocalhostResolver {

    private static final String COMPUTERNAME_ENV = "COMPUTERNAME";
    private static final String HOSTNAME_ENV = "HOSTNAME";


    public static String getLocalHostName() {
        return ObjectUtil.firstNonNull(
                getLocalHostNameViaInetAddress(),
                EnvironmentUtil.getEnvSafe(COMPUTERNAME_ENV),
                EnvironmentUtil.getEnvSafe(HOSTNAME_ENV)
        );
    }

    private static String getLocalHostNameViaInetAddress() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (!StringUtil.isNullOrEmpty(host)) {
                return host;
            }
        } catch (UnknownHostException e) {
            /* use alternate ways */
        }
        return null;
    }

    private LocalhostResolver() {
    }
}
