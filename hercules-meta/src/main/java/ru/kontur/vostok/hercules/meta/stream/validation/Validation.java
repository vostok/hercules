package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Optional;

/**
 * @author Petr Demenev
 */
public class Validation {

    public static Result check(Stream stream) {
        final Optional<String> streamError = StreamValidator.correct().validate(stream);
        return streamError.<Result>map(Result::error).orElseGet(Result::ok);
    }
}
