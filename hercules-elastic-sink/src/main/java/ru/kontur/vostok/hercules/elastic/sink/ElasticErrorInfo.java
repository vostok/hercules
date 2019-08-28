package ru.kontur.vostok.hercules.elastic.sink;

public class ElasticErrorInfo {
    private final ElasticResponseHandler.ErrorType type;
    private final String eventId;
    private final String index;
    private final String reason;

    public ElasticErrorInfo(ElasticResponseHandler.ErrorType type, String eventId, String index, String reason) {
        this.type = type;
        this.eventId = eventId;
        this.index = index;
        this.reason = reason;
    }

    public ElasticResponseHandler.ErrorType getType() {
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
