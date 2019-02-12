package ru.kontur.vostok.hercules.meta.stream.validation;

import org.junit.Test;

import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.validation.Validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StreamValidatorsTest {

    @Test
    public void shouldValidateBaseStream() {
        Stream stream = new BaseStream();
        stream.setName("test_t1_1");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidators.streamValidatorForHandler();

        assertFalse(validator.validate(stream).isPresent());
    }

    @Test
    public void shouldValidateInvalidBaseStream() {
        Stream stream = new BaseStream();
        stream.setName("test_t1_1");
        stream.setPartitions(0);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidators.streamValidatorForHandler();

        assertEquals("Partition is invalid: Value should be positive integer but was 0",
                validator.validate(stream).get());
    }

    @Test
    public void shouldValidateDerivedStream() {
        DerivedStream stream = new DerivedStream();
        stream.setName("test_t1_1");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        String[] streams = {"test_t1_2", "TEST_t1_3"};
        stream.setStreams(streams);
        Validator<Stream> validator = StreamValidators.streamValidatorForHandler();

        assertEquals("One of source streams is invalid: "
                        + "String should match the pattern '[a-z0-9_]{1,48}' but was 'TEST_t1_3'",
                validator.validate(stream).get());
    }
}
