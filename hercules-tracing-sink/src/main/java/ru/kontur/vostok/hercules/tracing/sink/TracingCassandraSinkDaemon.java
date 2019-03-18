package ru.kontur.vostok.hercules.tracing.sink;

import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.common.sink.AbstractCassandraSender;
import ru.kontur.vostok.hercules.cassandra.common.sink.AbstractCassandraSinkDaemon;

/**
 * TracingCassandraSinkDaemon
 *
 * @author Kirill Sulim
 */
public class TracingCassandraSinkDaemon extends AbstractCassandraSinkDaemon {

    public static void main(String[] args) {
        new TracingCassandraSinkDaemon().run(args);
    }

    @Override
    protected AbstractCassandraSender createCassandraSender(Session session) {
        return new TracingCassandraSender(session);
    }

    @Override
    protected String getDaemonName() {
        return "Cassandra tracing sink";
    }

    @Override
    protected String getDaemonId() {
        return "sink.cassandra.tracing";
    }
}
