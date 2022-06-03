package ru.kontur.vostok.hercules.graphite.sink.acl;

/**
 * {@link Statement#PERMIT} is used to pass an event through a filter<br>
 * {@link Statement#DENY} is used to filter out event.
 *
 * @author Vladimir Tsypaev
 */
public enum Statement {
    PERMIT,
    DENY,
}
