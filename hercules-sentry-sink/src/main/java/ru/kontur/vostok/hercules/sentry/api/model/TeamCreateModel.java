package ru.kontur.vostok.hercules.sentry.api.model;

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
