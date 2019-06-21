package ru.kontur.vostok.hercules.timeline.manager;

import com.datastax.oss.driver.api.core.CqlSession;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;

/**
 * @author Gregory Koshelev
 */
public class CassandraManager {
    private final CassandraConnector connector;

    public CassandraManager(CassandraConnector connector) {
        this.connector = connector;
    }

    /**
     * Create the table with TTL if it doesn't exist
     *
     * @param table the table name
     * @param ttl   TTL in millis
     */
    public void createTable(String table, long ttl) {
        CqlSession session = connector.session();

        //TODO: What if table already exists?
        // Create table if it doesn't exist
        session.execute("CREATE TABLE IF NOT EXISTS " + table + " (\n"
                + "slice int,"
                + "tt_offset bigint,"
                + "event_id blob,"
                + "payload blob,"
                + "PRIMARY KEY ((slice, tt_offset), event_id))\n"
                + "WITH default_time_to_live = " + (ttl / 1000) + ";");
    }

    /**
     * Delete the table if exists
     *
     * @param table the table name
     */
    public void deleteTable(String table) {
        CqlSession session = connector.session();

        session.execute("DROP TABLE IF EXISTS " + table);
    }

    /**
     * Alter ttl in the table
     *
     * @param table the table name
     * @param ttl TTL in millis
     */
    public void changeTtl(String table, long ttl) {
        CqlSession session = connector.session();

        session.execute("ALTER TABLE " + table + " WITH default_time_to_live = " + (ttl / 1000) + ";");
    }
}
