package ru.kontur.vostok.hercules.logger.logback;

/**
 * Configuration for {@link LogbackHttpAppender}
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpConfiguration {
    private String name;
    private String stream;
    private Integer batchSize;
    private Integer capacity;
    private Integer periodMillis;
    private Boolean loseOnOverflow;

    public LogbackHttpConfiguration(String name, String stream,
                                    Integer batchSize,
                                    Integer capacity,
                                    Integer periodMillis,
                                    Boolean loseOnOverflow) {
        this.name = name;
        this.stream = stream;
        this.batchSize = batchSize;
        this.capacity = capacity;
        this.periodMillis = periodMillis;
        this.loseOnOverflow = loseOnOverflow;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

