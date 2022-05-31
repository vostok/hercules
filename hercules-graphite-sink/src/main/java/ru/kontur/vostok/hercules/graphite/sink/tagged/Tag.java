package ru.kontur.vostok.hercules.graphite.sink.tagged;

import java.util.Objects;

/**
 * Tag entity.
 *
 * @author Aleksandr Yuferov
 */
class Tag {
    private final String key;
    private final String value;

    Tag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Tag tag = (Tag) other;
        return key.equals(tag.key) && value.equals(tag.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
