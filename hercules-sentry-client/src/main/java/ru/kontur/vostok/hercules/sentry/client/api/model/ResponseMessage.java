package ru.kontur.vostok.hercules.sentry.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The model of response message from Sentry
 *
 * @author Petr Demenev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseMessage {

    private String detail;

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
