package ru.kontur.vostok.hercules.logger.logback;

public class LogbackHttpConfiguration {
    private String url;
    private String apiKey;
    private String stream;
    private Integer batchSize;
    private Integer threads;
    private Integer capacity;
    private Integer periodMillis;
    private Boolean loseOnOverflow;

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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getPeriodMillis() {
        return periodMillis;
    }

    public void setPeriodMillis(Integer periodMillis) {
        this.periodMillis = periodMillis;
    }

    public Boolean getLoseOnOverflow() {
        return loseOnOverflow;
    }

    public void setLoseOnOverflow(Boolean loseOnOverflow) {
        this.loseOnOverflow = loseOnOverflow;
    }
}

