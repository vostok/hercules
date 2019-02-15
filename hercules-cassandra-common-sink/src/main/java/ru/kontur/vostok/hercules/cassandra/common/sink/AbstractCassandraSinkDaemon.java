package ru.kontur.vostok.hercules.cassandra.common.sink;

import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.kafka.util.processing.ServicePinger;
import ru.kontur.vostok.hercules.kafka.util.processing.single.AbstractSingleSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public abstract class AbstractCassandraSinkDaemon extends AbstractSingleSinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCassandraSinkDaemon.class);

    private CassandraConnector cassandraConnector;

    @Override
    protected SingleSender<UUID, Event> createSender(Properties sinkProperties) {
        return createCassandraSender(cassandraConnector.session());
    }

    abstract protected AbstractCassandraSender createCassandraSender(final Session session);

    @Override
    protected ServicePinger createPinger(Properties sinkProperties) {
        return () -> true;
    }

    @Override
    protected void initSink(Properties properties) {
        super.initSink(properties);

        final Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);

        cassandraConnector = new CassandraConnector(cassandraProperties);
        cassandraConnector.connect();
    }

    @Override
    protected void shutdownSink() {
        super.shutdownSink();

        try {
            if (Objects.nonNull(cassandraConnector)) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra connector sink", t);
        }
    }
}
