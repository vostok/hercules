package ru.kontur.vostok.hercules.sentry.api.model;

/**
 * The model of organization in Sentry.
 * It is a model which may be sent as JSON into Sentry for organization creation
 *
 * @author Petr Demenev
 */
public class Organization {

    private String name;
    private String slug;
    private boolean agreeTerms;

    public Organization(String name) {
        this.name = name;
        this.slug = name;
        this.agreeTerms = true;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isAgreeTerms() {
        return agreeTerms;
    }
}
