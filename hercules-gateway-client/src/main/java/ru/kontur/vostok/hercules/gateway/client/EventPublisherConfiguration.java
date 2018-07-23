package ru.kontur.vostok.hercules.gateway.client;

/**
 * Configuration class for EventPublisher
 *
 * @author Daniil Zhenikhov
 */
public class EventPublisherConfiguration {
    public final static EventPublisherConfiguration DefaultConfiguration = getDefaultConfig();

    private long periodMillis;
    private int batchSize;
    private int threads;
    private int capacity;
    private String url;
    private String apiKey;

    public EventPublisherConfiguration(
            long periodMillis,
            int batchSize,
            int threads,
            int capacity,
            String url,
            String apiKey) {
        this.periodMillis = periodMillis;
        this.batchSize = batchSize;
        this.threads = threads;
        this.capacity = capacity;
        this.url = url;
        this.apiKey = apiKey;
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

    public void setPeriodMillis(long periodMillis) {
        this.periodMillis = periodMillis;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private static EventPublisherConfiguration getDefaultConfig() {
        return new EventPublisherConfiguration(
                1000,
                100,
                3,
                10_000_000,
                "http://localhost:6306",
                "test-api-key"
        );
    }
}
