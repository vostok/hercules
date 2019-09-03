package ru.kontur.vostok.hercules.elastic.sink;

public class ErrorInfo {
    private final ErrorType type;
    private final String eventId;
    private final String index;
    private final String reason;

    public ErrorInfo(ErrorType type, String eventId, String index, String reason) {
        this.type = type;
        this.eventId = eventId;
        this.index = index;
        this.reason = reason;
    }

    public ErrorType getType() {
        return type;
    }

    public String getEventId() {
        return eventId;
    }

    public String getIndex() {
        return index;
    }

    public String getReason() {
        return reason;
    }

}
