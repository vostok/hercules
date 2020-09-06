package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.elastic.adapter.format.ProtoContainer;
import ru.kontur.vostok.hercules.json.Document;

/**
 * Maps part of the JSON-document to the event's payload.
 *
 * @author Gregory Koshelev
 * @see Document
 */
public interface Mapper {
    void map(Document src, ProtoContainer dest);
}
