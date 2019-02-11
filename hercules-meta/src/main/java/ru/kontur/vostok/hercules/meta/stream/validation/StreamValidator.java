package ru.kontur.vostok.hercules.meta.stream.validation;

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
public class StreamValidator {

    private final static String regex = "[a-z0-9_]{1,48}";

    public static Validator<Stream> correct() {
        return stream -> {
            if(stream instanceof DerivedStream) {
                for(String streamName : ((DerivedStream) stream).getStreams()) {
                    final Optional<String> nameError = StringValidators.matchesWith(regex).validate(streamName);
                    if(nameError.isPresent()) {
                        return Optional.of("One of source streams is invalid: " + nameError.get());
                    }
                }
            }
            final Optional<String> nameError = StringValidators.matchesWith(regex).validate(stream.getName());
            if(nameError.isPresent()) {
                return Optional.of("Name is invalid: " + nameError.get());
            }
            final Optional<String> partitionError = IntegerValidators.positive().validate(stream.getPartitions());
            if(partitionError.isPresent()) {
                return Optional.of("Partition is invalid: " + partitionError.get());
            }
            final Optional<String> ttlError = LongValidators.positive().validate(stream.getTtl());
            if(ttlError.isPresent()){
                return Optional.of("Ttl is invalid: " + ttlError.get());
            }
            return Optional.empty();
        };
    }
}


