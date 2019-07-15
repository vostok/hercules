package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.Optional;

/**
 * @author Petr Demenev
 */
public final class StreamValidators {

    private static final String regex = "[a-z0-9_]{1,48}";

    private static final Validator<String> nameValidator = StringValidators.matchesWith(regex);
    private static final Validator<Integer> partitionValidator = IntegerValidators.positive();
    private static final Validator<Long> ttlValidator = LongValidators.positive();

    private static final Validator<BaseStream> baseStreamValidator = baseStreamValidator();
    private static final Validator<DerivedStream> derivedStreamValidator = derivedStreamValidator();

    /**
     * @return stream validator
     */
    public static Validator<Stream> streamValidatorForHandler() {
        return stream -> {
            if (stream instanceof BaseStream) {
                return baseStreamValidator.validate((BaseStream) stream);
            } else if (stream instanceof DerivedStream) {
                return derivedStreamValidator.validate((DerivedStream) stream);
            }
            throw new NotImplementedException(String.format(
                    "Unknown stream type '%s'", stream.getClass().getCanonicalName()
            ));
        };
    }

    private static <T extends Stream> Validator<T> streamValidator() {
        return stream -> {
            ValidationResult result = nameValidator.validate(stream.getName());
            if (result.isError()) {
                return ValidationResult.error("Name is invalid: " + result.error());
            }

            result = partitionValidator.validate(stream.getPartitions());
            if (result.isError()) {
                return ValidationResult.error("Partition is invalid: " + result.error());
            }

            result = ttlValidator.validate(stream.getTtl());
            if (result.isError()) {
                return ValidationResult.error("Ttl is invalid: " + result.error());
            }

            return ValidationResult.ok();
        };
    }

    private static Validator<BaseStream> baseStreamValidator() {
        return streamValidator();
    }

    private static Validator<DerivedStream> derivedStreamValidator() {
        final Validator<DerivedStream> streamValidator = streamValidator();
        final Validator<DerivedStream> baseStreamNamesValidator = baseStreamNamesValidator();

        return stream -> {
            ValidationResult result = streamValidator.validate(stream);
            if (result.isError()) {
                return result;
            }

            result = baseStreamNamesValidator.validate(stream);
            return result;
        };
    }

    private static Validator<DerivedStream> baseStreamNamesValidator() {
        return stream -> {
            for (String streamName : stream.getStreams()) {
                final ValidationResult result = nameValidator.validate(streamName);
                if (result.isError()) {
                    return ValidationResult.error("One of source streams is invalid: " + result.error());
                }
            }
            return ValidationResult.ok();
        };
    }

    private StreamValidators() {
        /* static class */
    }
}
