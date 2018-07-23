package ru.kontur.vostok.hercules.management.api.cassandra;

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
                + "slice_id varchar,"
                + "tt_offset bigint,"
                + "event_timestamp bigint,"
                + "event_id bigint,"
                + "payload blob,"
                + "PRIMARY KEY ((slice_id, tt_offset), event_timestamp, event_id))");
    }

    public void deleteTable(String table) {
        Session session = connector.session();

        session.execute("DROP TABLE IF EXISTS " + table);
    }
}