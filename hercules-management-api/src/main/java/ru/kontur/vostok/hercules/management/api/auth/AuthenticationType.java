package ru.kontur.vostok.hercules.management.api.auth;

/**
 * The type of authentication.
 *
 * @author Gregory Koshelev
 */
public enum AuthenticationType {
    /**
     * Authenticated by master api key
     */
    MASTER,
    /**
     * Authenticated by ordinary api key
     */
    ORDINARY,
    /**
     * Not authenticated
     */
    NOT_AUTHENTICATED;
}
