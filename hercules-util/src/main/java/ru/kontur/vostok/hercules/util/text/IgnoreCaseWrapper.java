package ru.kontur.vostok.hercules.util.text;

/**
 * {@link CharSequence} wrapper for {@link java.util.HashMap} for case insensitivity.
 *
 * @param <T> Concrete {@link CharSequence} implementation.
 * @author Aleksandr Yuferov
 */
public class IgnoreCaseWrapper<T extends CharSequence> {
    private final T value;
    private final int hash;

    /**
     * Constructor.
     *
     * @param value Wrapped value.
     */
    public IgnoreCaseWrapper(T value) {
        this.value = value;
        this.hash = calcHash();
    }

    /**
     * Get wrapped value.
     *
     * @return Value.
     */
    public T value() {
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
        var that = (IgnoreCaseWrapper<?>) other;
        return StringUtil.equalsIgnoreCase(value, that.value);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "IgnoreCaseWrapper [ " + value + " ]";
    }

    private int calcHash() {
        return value.chars()
                .map(Character::toLowerCase)
                .reduce(0, (acc, curr) -> 31 * acc + curr);
    }
}
