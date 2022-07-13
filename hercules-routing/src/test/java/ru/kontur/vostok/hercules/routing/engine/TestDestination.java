package ru.kontur.vostok.hercules.routing.engine;

import ru.kontur.vostok.hercules.routing.Destination;
import ru.kontur.vostok.hercules.routing.interpolation.Interpolator;

import java.util.Objects;

/**
 * Destination implementation for tests.
 *
 * @author Aleksandr Yuferov
 */
public class TestDestination implements Destination<TestDestination> {
    private final String value;

    public static TestDestination of(String value) {
        return new TestDestination(value);
    }

    protected TestDestination(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public TestDestination interpolate(Interpolator interpolator, Interpolator.Context intContext) {
        return new TestDestination(interpolator.interpolate(value, intContext));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        TestDestination that = (TestDestination) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
