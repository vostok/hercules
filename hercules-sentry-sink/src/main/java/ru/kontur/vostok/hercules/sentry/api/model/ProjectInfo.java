package ru.kontur.vostok.hercules.sentry.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ProjectInfo
 * The project in Sentry.
 * The project contains a link to its organization.
 *
 * @author Kirill Sulim
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInfo {

    private String slug;
    private OrganizationInfo organization;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public OrganizationInfo getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationInfo organization) {
        this.organization = organization;
    }
}
