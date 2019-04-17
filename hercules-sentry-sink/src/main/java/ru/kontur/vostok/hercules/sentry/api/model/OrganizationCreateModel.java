package ru.kontur.vostok.hercules.sentry.api.model;

public class OrganizationCreateModel {

    private String name;
    private String slug;
    private String agreeTerms;

    public OrganizationCreateModel(String name) {
        this.name = name;
        this.slug = name;
        this.agreeTerms = "true";
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getAgreeTerms() {
        return agreeTerms;
    }
}
