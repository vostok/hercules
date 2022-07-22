package ru.kontur.vostok.hercules.sentry.client.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The model of project in Sentry.
 * It is a model which may be got from JSON in Sentry response
 *
 * @author Kirill Sulim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInfo {

    private String slug;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
