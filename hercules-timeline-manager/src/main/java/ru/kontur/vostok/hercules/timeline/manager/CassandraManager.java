package ru.kontur.vostok.hercules.timeline.manager;

import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;

/**
 * @author Gregory Koshelev
 */
public class CassandraManager {
    private final CassandraConnector connector;

    public CassandraManager(CassandraConnector connector) {
        this.connector = connector;
    }

    public void createTable(String table) {
        Session session = connector.session();

        //TODO: What if table already exists?
        // Create table if it doesn't exist
        session.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "slice int,"
                + "tt_offset bigint,"
                + "event_id blob,"
                + "payload blob,"
                + "PRIMARY KEY ((slice, tt_offset), event_id));");
    }

    public void deleteTable(String table) {
        Session session = connector.session();

        session.execute("DROP TABLE IF EXISTS " + table);
    }
}
