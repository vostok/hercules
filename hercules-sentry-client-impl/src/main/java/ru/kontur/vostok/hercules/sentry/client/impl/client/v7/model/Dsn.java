package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Objects;
import java.util.function.Consumer;

public class Dsn {

    private static final Dsn UNAVAILABLE = new Dsn(null, null, d -> {
    });

    private final String publicKey;
    private final String projectId;
    private final Consumer<Dsn> discardToken;

    public Dsn(String publicKey, String projectId, Consumer<Dsn> discardToken) {
        this.publicKey = publicKey;
        this.projectId = projectId;
        this.discardToken = discardToken;
    }

    public static Dsn unavailable() {
        return UNAVAILABLE;
    }

    public boolean isUnavailable() {
        return publicKey == null || projectId == null;
    }

    public void discard() {
        discardToken.accept(this);
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public String getProjectId() {
        return this.projectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dsn dsn = (Dsn) o;
        return Objects.equals(publicKey, dsn.publicKey) && Objects.equals(projectId,
                dsn.projectId) && Objects.equals(discardToken, dsn.discardToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKey, projectId, discardToken);
    }
}
