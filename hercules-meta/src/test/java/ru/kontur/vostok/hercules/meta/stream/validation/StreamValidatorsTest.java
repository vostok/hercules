package ru.kontur.vostok.hercules.meta.stream.validation;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.validation.Validator;

public class StreamValidatorsTest {

    @Test
    public void shouldValidateStreamName() {
        Stream stream = new BaseStream();
        stream.setName("stream&");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertFalse(validator.validate(stream).isOk());
    }

    @Test
    public void shouldValidateStreamPartition() {
        Stream stream = new BaseStream();
        stream.setName("stream");
        stream.setPartitions(50);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertFalse(validator.validate(stream).isOk());
    }

    @Test
    public void shouldValidateStreamTtl() {
        Stream stream = new BaseStream();
        stream.setName("stream");
        stream.setPartitions(1);
        stream.setTtl(-1);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertFalse(validator.validate(stream).isOk());
    }

    @Test
    public void shouldValidateStreamDescription() {
        Stream stream = new BaseStream();
        stream.setName("stream");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        stream.setDescription(StringUtil.repeat(' ', 1001));
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertFalse(validator.validate(stream).isOk());
    }

    @Test
    public void shouldValidateDerivedStream() {
        DerivedStream stream = new DerivedStream();
        stream.setName("stream");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        String[] streams = {"stream1?", "stream2?"};
        stream.setStreams(streams);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertFalse(validator.validate(stream).isOk());
    }

    @Test
    public void shouldBeCorrectDerivedStream() {
        DerivedStream stream = new DerivedStream();
        stream.setName("stream");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        String[] streams = {"stream1", "stream2"};
        stream.setStreams(streams);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertTrue(validator.validate(stream).isOk());
    }

    @Test
    public void shouldBeCorrectBaseStream() {
        Stream stream = new BaseStream();
        stream.setName("stream");
        stream.setPartitions(1);
        stream.setTtl(86400000);
        Validator<Stream> validator = StreamValidators.STREAM_VALIDATOR;

        Assert.assertTrue(validator.validate(stream).isOk());
    }
}
