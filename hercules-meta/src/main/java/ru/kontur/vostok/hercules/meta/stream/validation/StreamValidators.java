package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
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
            if(stream instanceof BaseStream) {
                return baseStreamValidator.validate((BaseStream) stream);
            } else if(stream instanceof DerivedStream) {
                return derivedStreamValidator.validate((DerivedStream) stream);
            }
            throw new NotImplementedException(String.format(
                    "Unknown stream type '%s'", stream.getClass().getCanonicalName()
            ));
        };
    }

    private static <T extends Stream> Validator<T> streamValidator() {
        return stream -> {
            final Optional<String> nameError = nameValidator.validate(stream.getName());
            if(nameError.isPresent()) {
                return Optional.of("Name is invalid: " + nameError.get());
            }

            final Optional<String> partitionError = partitionValidator.validate(stream.getPartitions());
            if(partitionError.isPresent()) {
                return Optional.of("Partition is invalid: " + partitionError.get());
            }

            final Optional<String> ttlError = ttlValidator.validate(stream.getTtl());
            if(ttlError.isPresent()){
                return Optional.of("Ttl is invalid: " + ttlError.get());
            }

            return Optional.empty();
        };
    }

    private static Validator<BaseStream> baseStreamValidator() {
        return streamValidator();
    }

    private static Validator<DerivedStream> derivedStreamValidator() {
        final Validator<DerivedStream> streamValidator = streamValidator();
        final Validator<DerivedStream> baseStreamNamesValidator = baseStreamNamesValidator();

        return stream -> {
            final Optional<String> streamError = streamValidator.validate(stream);
            if(streamError.isPresent()) {
                return streamError;
            }

            final Optional<String> baseStreamsNamesError = baseStreamNamesValidator.validate(stream);
            if (baseStreamsNamesError.isPresent()) {
                return baseStreamsNamesError;
            }

            return Optional.empty();
        };
    }

    private static Validator<DerivedStream> baseStreamNamesValidator() {
        return stream -> {
            for(String streamName : stream.getStreams()) {
                final Optional<String> nameError = nameValidator.validate(streamName);
                if(nameError.isPresent()) {
                    return Optional.of("One of source streams is invalid: " + nameError.get());
                }
            }
            return Optional.empty();
        };
    }

    private StreamValidators() {
        /* static class */
    }
}
