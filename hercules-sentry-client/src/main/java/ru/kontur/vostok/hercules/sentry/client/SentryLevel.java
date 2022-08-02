package ru.kontur.vostok.hercules.sentry.client;

/**
 * Levels of log available in Sentry.
 *
 * @author Tatyana Tokmyanina
 */
public enum SentryLevel {

    /**
     * Fatal is the highest form of log available, use it for unrecoverable issues.
     */
    FATAL,
    /**
     * Error denotes an unexpected behaviour that prevented the code to work properly.
     */
    ERROR,
    /**
     * Warning should be used to define logs generated by expected and handled bad behaviour.
     */
    WARNING,
    /**
     * Info is used to give general details on the running application, usually only messages.
     */
    INFO,
    /**
     * Debug information to track every detail of the application execution process.
     */
    DEBUG;
}