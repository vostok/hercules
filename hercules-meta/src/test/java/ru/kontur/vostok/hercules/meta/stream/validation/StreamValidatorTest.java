package ru.kontur.vostok.hercules.meta.stream.validation;

import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.validation.Validator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StreamValidatorTest {

    @Test
    public void shouldValidateBaseStream() {
        Stream stream = new BaseStream();
        stream.setName("test_t1_1");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidator.correct();
        assertFalse(validator.validate(stream).isPresent());

        stream.setPartitions(0);
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
        Validator<Stream> validator = StreamValidator.correct();
        assertEquals("One of source streams is invalid: "
                        + "String should match the pattern but was 'TEST_t1_3'",
                validator.validate(stream).get());
    }
}