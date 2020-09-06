package ru.kontur.vostok.hercules.elastic.adapter.index;

/**
 * Timetamp format in the JSON-document
 *
 * @author Gregory Koshelev
 */
public enum TimestampFormat {
    /**
     * ISO 8601 compatible format (i.e. {@link java.time.format.DateTimeFormatter#ISO_DATE_TIME})
     */
    ISO_DATETIME,
    /**
     * Unix time format (seconds passed from 1970-01-01 00:00:00)
     */
    UNIX_TIME
}
