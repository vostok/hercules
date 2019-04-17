package ru.kontur.vostok.hercules.sentry.api.model;

/**
 * The model of team in Sentry.
 * It is a model which may be sent as JSON into Sentry for team creation
 *
 * @author Petr Demenev
 */
public class TeamCreateModel {

    private String name;
    private String slug;

    public TeamCreateModel(String name) {
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
