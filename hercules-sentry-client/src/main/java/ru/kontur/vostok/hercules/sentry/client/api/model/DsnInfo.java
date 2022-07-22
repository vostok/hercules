package ru.kontur.vostok.hercules.sentry.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The model of project DSN in Sentry.
 * It is a model which may be got from JSON in Sentry response.
 * <p>
 * The DSN (Data Source Name) is a client key of the Sentry project.
 * It looks a lot like a standard URL.
 * It includes the protocol, public key, the server address, and the project identifier.
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
