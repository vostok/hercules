package ru.kontur.vostok.hercules.util.functional;

import javax.swing.text.html.Option;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Result
 *
 * @author Kirill Sulim
 */
public class Result<R, E> {

    private final R result;
    private final E error;

    private Result(R result, E error) {
        this.result = result;
        this.error = error;
    }

    public boolean isOk() {
        return Objects.isNull(error);
    }

    public R get() {
        return result;
    }

    public E getError() {
        return error;
    }

    public <R1> Result<R1, E> map(Function<R, R1> converter) {
        return map(converter, Function.identity());
    }

    public <R1, E1> Result<R1, E1> map(Function<R, R1> converter, Function<E, E1> errorConverter) {
        if (isOk()) {
            return Result.ok(converter.apply(result));
        } else {
            return Result.error(errorConverter.apply(error));
        }
    }

    public <E1> Result<R, E1> mapError(Function<E, E1> errorConverter) {
        return map(Function.identity(), errorConverter);
    }

    public <R1> Result<R1, E> flatMap(final Function<R, Result<R1, E>> converter) {
        if (isOk()) {
            return converter.apply(result);
        } else {
            return Result.error(error);
        }
    }

    public static <R, E> Result<R, E> ok(R result) {
        return new Result<>(result, null);
    }

    public static <E> Result<Void, E> ok() {
        return new Result<>(null, null);
    }

    public static <R, E> Result<R, E> error(E error) {
        return new Result<>(null, error);
    }

    public static <R> Result<R, Exception> of(final Callable<R> callable) {
        try {
            return Result.ok(callable.call());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
}
