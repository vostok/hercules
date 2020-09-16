package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.validation.LongValidators;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

/**
 *
 * @author Petr Demenev
 */
public final class StreamValidators {

    public static final Validator<String> DESCRIPTION_VALIDATOR =
            (desc) -> (desc == null || desc.length() < 1_000)
                    ? ValidationResult.ok()
                    : ValidationResult.error("The length of description has exceeded the limit of 1000");

    public static final Validator<Integer> PARTITION_VALIDATOR =
            (partition) -> (partition > 0 && partition <= 48)
                    ? ValidationResult.ok()
                    : ValidationResult.error("Value should be >0 and <=48");

    public static final Validator<Long> TTL_VALIDATOR = LongValidators.positive();

    public static final Validator<String> NAME_VALIDATOR = StringValidators.matchesWith("[a-z0-9_]{1,48}");

    public static final Validator<Stream> STREAM_VALIDATOR = streamValidator();

    private static <T extends Stream> Validator<T> streamValidator() {
        return stream -> {
            ValidationResult result = NAME_VALIDATOR.validate(stream.getName());
            if (result.isError()) {
                return ValidationResult.error("Name is invalid: " + result.error());
            }

            result = PARTITION_VALIDATOR.validate(stream.getPartitions());
            if (result.isError()) {
                return ValidationResult.error("Partition is invalid: " + result.error());
            }

            result = TTL_VALIDATOR.validate(stream.getTtl());
            if (result.isError()) {
                return ValidationResult.error("Ttl is invalid: " + result.error());
            }

            result = DESCRIPTION_VALIDATOR.validate(stream.getDescription());
            if (result.isError()) {
                return ValidationResult.error("Description is invalid: " + result.error());
            }

            if (stream instanceof DerivedStream){
                for (String streamName : ((DerivedStream) stream).getStreams()) {
                    result = NAME_VALIDATOR.validate(streamName);
                    if (result.isError()) {
                        return ValidationResult.error("One of source streams is invalid: " + result.error());
                    }
                }
            }

            return ValidationResult.ok();
        };
    }

    private StreamValidators() {
        /* static class */
    }
}
