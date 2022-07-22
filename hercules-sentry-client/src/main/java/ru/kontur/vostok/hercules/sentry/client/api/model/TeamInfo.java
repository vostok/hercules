package ru.kontur.vostok.hercules.sentry.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The model of team in Sentry.
 * It is a model which may be got from JSON in Sentry response
 *
 * @author Petr Demenev
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamInfo {

    private String slug;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
