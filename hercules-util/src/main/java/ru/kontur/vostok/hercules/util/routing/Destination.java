package ru.kontur.vostok.hercules.util.routing;

import ru.kontur.vostok.hercules.util.routing.interpolation.Interpolator;

/**
 * Interface for destination.
 *
 * @param <D> Concrete type of Destination object.
 * @author Aleksandr Yuferov
 */
public interface Destination<D extends Destination<D>> {

    /**
     * Perform interpolation.
     * <p/>
     * Create the copy of this object with interpolated fields.
     * To perform interpolation-transformations this method should use {@link Interpolator} and it's context.
     *
     * @param interpolator Object that performs transformations.
     * @param intContext   Context for interpolator object.
     * @return Copy of object with interpolated fields.
     */
    D interpolate(Interpolator interpolator, Interpolator.Context intContext);
}
