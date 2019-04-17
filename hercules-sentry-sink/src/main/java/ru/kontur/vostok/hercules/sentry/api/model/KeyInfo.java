package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The key containing DSN.
 * It is a model which may be got from JSON in Sentry response.
 *
 * @author Kirill Sulim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyInfo {

    private DsnInfo dsn;

    public DsnInfo getDsn() {
        return dsn;
    }

    public void setDsn(DsnInfo dsn) {
        this.dsn = dsn;
    }
}
