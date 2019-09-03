package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

class EventWrapper {

    private final Event event;
    private final String id;
    private final String index;

    EventWrapper(Event event) {
        this.event = event;
        this.id = EventUtil.extractStringId(event);
        this.index = IndexToElasticJsonWriter.extractIndex(event).orElse(null);
    }

    Event getEvent() {
        return event;
    }

    String getId() {
        return id;
    }

    String getIndex() {
        return index;
    }

}
