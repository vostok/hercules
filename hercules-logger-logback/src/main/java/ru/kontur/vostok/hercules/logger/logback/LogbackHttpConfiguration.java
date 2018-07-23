package ru.kontur.vostok.hercules.logger.logback;

/**
 * Configuration for {@link LogbackHttpAppender}
 *
 * @author Daniil Zhenikhov
 */
public class LogbackHttpConfiguration {
    private String stream;
    private Boolean loseOnOverflow;

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public void setLoseOnOverflow(Boolean loseOnOverflow) {
        this.loseOnOverflow = loseOnOverflow;
    }

    public Boolean getLoseOnOverflow() {
        return loseOnOverflow;
    }
}

