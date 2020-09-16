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
    private boolean isActive;

    public DsnInfo getDsn() {
        return dsn;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setDsn(DsnInfo dsn) {
        this.dsn = dsn;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
