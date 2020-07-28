package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Map;

/**
 * Maps part of event to JSON-document.
 *
 * @author Gregory Koshelev
 * @see Document
 */
public interface Mapper {
    void map(Event event, Document document);
}
