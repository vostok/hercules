package ru.kontur.vostok.hercules.cassandra.common.sink;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstract cassandra sender set the main pipeline for any cassandra sink. In most cases all cassandra sink services
 * should extends this class.
 */
public abstract class AbstractCassandraSender implements SingleSender<UUID, Event> {

    private final Session session;
    private final PreparedStatement prepared;

    public AbstractCassandraSender(final Session session) {
        this.session = session;
        this.prepared = session.prepare(getPreparedStatement());
    }

    @Override
    public boolean process(UUID uuid, Event event) throws BackendServiceFailedException {
        final Optional<Object[]> converted = convert(event);
        if (!converted.isPresent()) {
            return false;
        }

        final BoundStatement boundStatement = prepared.bind(converted.get());
        try {
            ResultSet result = session.execute(boundStatement);
            return true;
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }
    }

    /**
     * This method must be implemented in descendants and convert hercules event to array of values for cassandra row.
     * In case of invalid event Optional.empty() must be returned.
     *
     * @param event hercules event
     * @return Optional of array of values or empty optional
     */
    abstract protected Optional<Object[]> convert(final Event event);

    /**
     * This method must be implemented in descendants and return prepared statement for insert row of values.
     *
     * @return prepared statement
     */
    abstract protected String getPreparedStatement();

    @Override
    public void close() throws Exception {
        session.close();
    }
}
