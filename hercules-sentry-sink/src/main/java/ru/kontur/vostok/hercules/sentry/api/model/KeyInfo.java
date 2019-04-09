package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * KeyInfo
 * The key with DSN
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
