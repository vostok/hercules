package ru.kontur.vostok.hercules.sentry.client.api.model;

/**
 * The model of project in Sentry.
 * It is a model which may be sent as JSON into Sentry for project creation
 *
 * @author Petr Demenev
 */
public class Project {

    private String name;
    private String slug;

    public Project(String name) {
        this.name = name;
        this.slug = name;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }
}
