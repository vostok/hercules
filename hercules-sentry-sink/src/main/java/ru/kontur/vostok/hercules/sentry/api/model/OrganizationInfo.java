package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The model of organization in Sentry.
 * It is a model which may be got from JSON in Sentry response
 *
 * @author Kirill Sulim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationInfo {

    private String slug;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
