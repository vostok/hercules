package ru.kontur.vostok.hercules.configuration;

/**
 * @author Gregory Koshelev
 */
public final class Scopes {
    public static final String ZOOKEEPER = "zk";
    public static final String KAFKA = "kafka";
    public static final String CASSANDRA = "cassandra";

    public static final String CURATOR = "curator";

    public static final String HTTP_SERVER = "http.server";

    public static final String CONSUMER = "consumer";
    public static final String PRODUCER = "producer";
    public static final String STREAMS = "streams";

    public static final String SINK = "sink";

    public static final String METRICS = "metrics";

    public static final String CONTEXT = "context";

    private Scopes() {

    }
}
