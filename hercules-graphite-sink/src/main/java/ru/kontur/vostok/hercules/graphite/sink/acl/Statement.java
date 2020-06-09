package ru.kontur.vostok.hercules.graphite.sink.acl;

/**
 * {@link Statement#PERMIT} is used to pass an event through a filter<br>
 * {@link Statement#DENY} is used to filter out event.
 *
 * @author Vladimir Tsypaev
 */
public enum Statement {
    PERMIT(true),
    DENY(false);

    private final boolean permit;

    Statement(boolean permit) {
        this.permit = permit;
    }

    public boolean isPermit() {
        return permit;
    }
}
