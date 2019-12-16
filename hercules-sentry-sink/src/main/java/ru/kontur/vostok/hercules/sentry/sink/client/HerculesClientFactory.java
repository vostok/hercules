package ru.kontur.vostok.hercules.sentry.sink.client;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.config.Lookup;
import io.sentry.connection.AsyncConnection;
import io.sentry.connection.Connection;
import io.sentry.connection.EventSampler;
import io.sentry.connection.HttpConnection;
import io.sentry.connection.ProxyAuthenticator;
import io.sentry.connection.RandomEventSampler;
import io.sentry.dsn.Dsn;
import io.sentry.dsn.InvalidDsnException;
import io.sentry.event.helper.ContextBuilderHelper;
import io.sentry.marshaller.Marshaller;
import io.sentry.marshaller.json.JsonMarshaller;
import io.sentry.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * HerculesClientFactory
 *
 * @author Petr Demenev
 */
public class HerculesClientFactory extends DefaultSentryClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HerculesClientFactory.class);

    private final int connectionTimeoutDefaultMs;
    private final int readTimeoutDefaultMs;
    private URI rewritingUri;

    public HerculesClientFactory(Properties senderProperties) {
        super();
        connectionTimeoutDefaultMs = PropertiesUtil.get(Props.CONNECTION_TIMEOUT_MS, senderProperties).get();
        readTimeoutDefaultMs = PropertiesUtil.get(Props.READ_TIMEOUT_MS, senderProperties).get();

        ParameterValue<String> rewritingUrlParam = PropertiesUtil.get(Props.SENTRY_REWRITING_URL, senderProperties);
        if (!rewritingUrlParam.isEmpty()) {
            try {
                rewritingUri = new URI(rewritingUrlParam.get());
            } catch (URISyntaxException e) {
                LOGGER.error("Cannot parse URI from value of parameter rewritingUrl: '{}'", rewritingUrlParam.get());
            }
        }
    }

    @Override
    protected JsonMarshaller createJsonMarshaller(int maxMessageLength) {
        return new HerculesJsonMarshaller(maxMessageLength);
    }

    /**
     * Create Sentry client
     * <p>
     * The overridden method with HerculesSentryClient instead of SentryClient
     *
     * @param dsn Data Source name allowing a direct connection to a Sentry server.
     * @return Sentry client matching dsn
     */
    @Override
    public SentryClient createSentryClient(Dsn dsn) {
        try {
            Dsn newDsn = modifyDsn(dsn);
            SentryClient sentryClient = new HerculesSentryClient(createConnection(newDsn), getContextManager(newDsn));
            sentryClient.addBuilderHelper(new ContextBuilderHelper(sentryClient));
            return configureSentryClient(sentryClient, newDsn);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize sentry", e);
            throw e;
        }
    }

    /**
     * Whether or not to wrap the underlying connection in an {@link AsyncConnection}.
     * But the overriding method always return false because
     * {@link AsyncConnection} is not needed and we need to forward exceptions to
     * {@link ru.kontur.vostok.hercules.sentry.sink.SentrySyncProcessor}
     *
     * @param dsn Sentry server DSN which may contain options (see the overridden method)
     * @return false (not to wrap the underlying connection in an {@link AsyncConnection})
     */
    @Override
    protected boolean getAsyncEnabled(Dsn dsn) {
        return false;
    }

    /**
     * Whether or not buffering is enabled.
     * But the overriding method always return false because
     * {@link io.sentry.connection.BufferedConnection} is not needed
     *
     * @param dsn Sentry server DSN which may contain options (see the overridden method)
     * @return false (not buffering is enabled)
     */
    @Override
    protected boolean getBufferEnabled(Dsn dsn) {
        return false;
    }

    /**
     * Timeout for requests to the Sentry server, in milliseconds.
     * The overridden method with new value of default connection timeout.
     *
     * @param dsn Sentry server DSN which may contain options.
     * @return Timeout for requests to the Sentry server, in milliseconds.
     */
    @Override
    protected int getTimeout(Dsn dsn) {
        return Util.parseInteger(Lookup.lookup(CONNECTION_TIMEOUT_OPTION, dsn), connectionTimeoutDefaultMs);
    }

    /**
     * Read timeout for requests to the Sentry server, in milliseconds.
     * The overridden method with new value of default read timeout.
     *
     * @param dsn Sentry server DSN which may contain options.
     * @return Read timeout for requests to the Sentry server, in milliseconds.
     */
    @Override
    protected int getReadTimeout(Dsn dsn) {
        return Util.parseInteger(Lookup.lookup(READ_TIMEOUT_OPTION, dsn), readTimeoutDefaultMs);
    }

    /**
     * Creates an HTTP connection to the Sentry server.
     * Overrides {@link DefaultSentryClientFactory#createHttpConnection(Dsn)}
     * with creating object of {@link HerculesHttpConnection} insteadof {@link HttpConnection}
     *
     * @param dsn Data Source Name of the Sentry server.
     * @return an {@link HttpConnection} to the server.
     */
    @Override
    protected Connection createHttpConnection(Dsn dsn) {
        URL sentryApiUrl = HttpConnection.getSentryApiUrl(dsn.getUri(), dsn.getProjectId());

        String proxyHost = getProxyHost(dsn);
        String proxyUser = getProxyUser(dsn);
        String proxyPass = getProxyPass(dsn);
        int proxyPort = getProxyPort(dsn);

        Proxy proxy = null;
        if (proxyHost != null) {
            InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
            if (proxyUser != null && proxyPass != null) {
                Authenticator.setDefault(new ProxyAuthenticator(proxyUser, proxyPass));
            }
        }

        Double sampleRate = getSampleRate(dsn);
        EventSampler eventSampler = null;
        if (sampleRate != null) {
            eventSampler = new RandomEventSampler(sampleRate);
        }

        HttpConnection httpConnection = new HerculesHttpConnection(sentryApiUrl, dsn.getPublicKey(),
                dsn.getSecretKey(), proxy, eventSampler);

        Marshaller marshaller = createMarshaller(dsn);
        httpConnection.setMarshaller(marshaller);

        int timeout = getTimeout(dsn);
        httpConnection.setConnectionTimeout(timeout);

        int readTimeout = getReadTimeout(dsn);
        httpConnection.setReadTimeout(readTimeout);

        boolean bypassSecurityEnabled = getBypassSecurityEnabled(dsn);
        httpConnection.setBypassSecurity(bypassSecurityEnabled);

        return httpConnection;
    }

    /**
     * Allows to modify dsn by rewriting protocol, host and port
     *
     * @param dsn original dsn
     * @return modified dsn
     */
    public Dsn modifyDsn(Dsn dsn) {
        if (rewritingUri == null) {
            return dsn;
        }
        URI newUri;
        try {
            newUri = new URI(
                    rewritingUri.getScheme(),
                    getUserInfo(dsn),
                    rewritingUri.getHost(),
                    rewritingUri.getPort(),
                    dsn.getPath() + dsn.getProjectId(),
                    getQuery(dsn),
                    null);
        } catch (URISyntaxException e) {
            throw new InvalidDsnException("Impossible to determine Sentry's URI from the DSN and replacing fields", e);
        }
        return new Dsn(newUri);
    }

    private String getUserInfo(Dsn dsn) {
        StringBuilder sb = new StringBuilder(dsn.getPublicKey());
        String secretKey = dsn.getSecretKey();
        if (!StringUtil.isNullOrEmpty(secretKey)) {
            sb.append(":").append(secretKey);
        }
        return sb.toString();
    }

    private String getQuery(Dsn dsn) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<String, String> entry : dsn.getOptions().entrySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static class Props {
        static final Parameter<Integer> CONNECTION_TIMEOUT_MS =
                Parameter.integerParameter("connectionTimeoutMs").
                        withDefault(1_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> READ_TIMEOUT_MS =
                Parameter.integerParameter("readTimeoutMs").
                        withDefault(5_000).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<String> SENTRY_REWRITING_URL =
                Parameter.stringParameter("sentry.rewritingUrl").
                        build();
    }
}
