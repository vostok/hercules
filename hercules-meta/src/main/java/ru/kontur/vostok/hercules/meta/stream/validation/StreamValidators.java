package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.Optional;

/**
 * @author Petr Demenev
 */
public final class StreamValidators {

    private final static String regex = "[a-z0-9_]{1,48}";

    private final static Validator<String> nameValidator = StringValidators.matchesWith(regex);
    private final static Validator<Integer> partitionValidator = IntegerValidators.positive();
    private final static Validator<Long> ttlValidator = LongValidators.positive();
    private final static Validator<Stream> streamValidator = streamValidator();
    private final static Validator<Stream> baseStreamValidator = baseStreamValidator();
    private final static Validator<Stream> derivedStreamValidator = derivedStreamValidator();

    public static Validator<Stream> streamValidatorForHandler() {
        return stream -> {
            if(stream instanceof BaseStream) {
                return baseStreamValidator.validate(stream);
            } else if(stream instanceof DerivedStream) {
                return derivedStreamValidator.validate(stream);
            }
            return Optional.of("Unknown stream type");
        };
    }

    private static Validator<Stream> streamValidator() {
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

    private static Validator<Stream> baseStreamValidator() {
        return streamValidator;
    }

    private static Validator<Stream> derivedStreamValidator() {
        return stream -> {
            final Optional<String> streamError = streamValidator.validate(stream);
            if(streamError.isPresent()) {
                return streamError;
            }
            for(String streamName : ((DerivedStream) stream).getStreams()) {
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
