package ru.kontur.vostok.hercules.routing;

import ru.kontur.vostok.hercules.routing.interpolation.Interpolator;

/**
 * @author Aleksandr Yuferov
 */
public interface Destination<D extends Destination<D>> {
    D interpolate(Interpolator interpolator, Interpolator.Context intContext);
}
