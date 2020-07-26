package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Map;

/**
 * Maps part of event to JSON-document.
 * <p>
 * JSON-document is represented by object model of nested {@link Map} objects.<br>
 * A key of an entry is the tag name.
 * A value of an entry is one of following types:
 * <ul>
 *   <li>Boxed primitive,
 *   <li>{@link String},
 *   <li>arrays of primitives,
 *   <li>{@link java.util.List} of {@link String} or {@link Map}
 *   <li>{@link Map}
 * </ul>
 *
 * @author Gregory Koshelev
 * @see ru.kontur.vostok.hercules.json.DocumentWriter
 */
public interface Mapper {
    void map(Event event, Map<String, Object> document);
}
