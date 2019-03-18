package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DsnInfo
 *
 * @author Kirill Sulim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DsnInfo {

    @JsonProperty("public")
    private String publicDsn;

    public String getPublicDsn() {
        return publicDsn;
    }

    public void setPublicDsn(String publicDsn) {
        this.publicDsn = publicDsn;
    }
}
