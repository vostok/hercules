package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OrganizationInfo
 * The organization in Sentry
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
