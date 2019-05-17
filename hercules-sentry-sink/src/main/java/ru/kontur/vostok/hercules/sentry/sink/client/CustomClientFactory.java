package ru.kontur.vostok.hercules.sentry.sink.client;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.connection.NoopConnection;
import io.sentry.context.ThreadLocalContextManager;
import io.sentry.dsn.Dsn;
import io.sentry.event.helper.ContextBuilderHelper;
import io.sentry.event.helper.HttpEventBuilderHelper;
import io.sentry.marshaller.json.JsonMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomClientFactory
 *
 * @author Kirill Sulim
 */
public class CustomClientFactory extends DefaultSentryClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(CustomClientFactory.class);

    @Override
    protected JsonMarshaller createJsonMarshaller(int maxMessageLength) {
        return new CustomJsonMarshaller(maxMessageLength);
    }

    /**
     * Create Sentry client
     * <p>
     * The overridden method with CustomSentryClient instead of SentryClient
     *
     * @param dsn Data Source name allowing a direct connection to a Sentry server.
     * @return Sentry client matching dsn
     */
    @Override
    public SentryClient createSentryClient(Dsn dsn) {
        try {
            CustomSentryClient sentryClient = new CustomSentryClient(createConnection(dsn), getContextManager(dsn));
            try {
                Class.forName("javax.servlet.ServletRequestListener", false, this.getClass().getClassLoader());
                sentryClient.addBuilderHelper(new HttpEventBuilderHelper());
            } catch (ClassNotFoundException e) {
                logger.debug("The current environment doesn't provide access to servlets,"
                        + " or provides an unsupported version.");
            }
            sentryClient.addBuilderHelper(new ContextBuilderHelper(sentryClient));
            return configureSentryClient(sentryClient, dsn);
        } catch (Exception e) {
            logger.error("Failed to initialize sentry, falling back to no-op client", e);
            return new CustomSentryClient(new NoopConnection(), new ThreadLocalContextManager());
        }
    }
}
