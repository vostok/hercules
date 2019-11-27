package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Collections;

public class EventReaderWriteReadTest {

    @Test
    public void shouldWriteReadAllTags() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader.readAllTags());

        final EventBuilder builder = EventBuilder.create(
                TimeUtil.millisToTicks(123_456_789L),
                UuidGenerator.getClientInstance().withTicks(TimeUtil.millisToTicks(123_456_789L))
        )
                .tag("string-tag", Variant.ofString("Abc ЕЁЮ"))
                .tag("flag-array-tag", Variant.ofVector(Vector.ofFlags(new boolean[]{true, true, false})));

        pipe.process(builder.build()).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadNoTags() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader.readNoTags());

        EventBuilder builder = EventBuilder.create(
                TimeUtil.millisToTicks(123_456_789L),
                UuidGenerator.getClientInstance().withTicks(TimeUtil.millisToTicks(123_456_789L))
        )
                .tag("string-tag", Variant.ofString("Abc ЕЁЮ"))
                .tag("flag-array-tag", Variant.ofVector(Vector.ofFlags(new boolean[]{true, true, false})));

        WriteReadPipe.ProcessedCapture<Event> capture = pipe.process(builder.build());

        Event processed = capture.getProcessed();
        Event original = capture.getOriginal();

        Assert.assertEquals(original.getVersion(), processed.getVersion());
        Assert.assertEquals(original.getTimestamp(), processed.getTimestamp());
        Assert.assertEquals(original.getUuid(), processed.getUuid());

        Assert.assertEquals(0, processed.getPayload().count());

        Assert.assertArrayEquals(original.getBytes(), processed.getBytes());
    }

    @Test
    public void shouldWriteReadOneTag() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader.readTags(Collections.singleton(TinyString.of("string-tag"))));

        EventBuilder builder = EventBuilder.create(
                TimeUtil.millisToTicks(123_456_789L),
                UuidGenerator.getClientInstance().withTicks(TimeUtil.millisToTicks(123_456_789L))
        )
                .tag("string-tag", Variant.ofString("Abc ЕЁЮ"))
                .tag("flag-array-tag", Variant.ofVector(Vector.ofFlags(true, true, false)));

        WriteReadPipe.ProcessedCapture<Event> capture = pipe.process(builder.build());

        Event processed = capture.getProcessed();
        Event original = capture.getOriginal();

        Assert.assertEquals(original.getVersion(), processed.getVersion());
        Assert.assertEquals(original.getTimestamp(), processed.getTimestamp());
        Assert.assertEquals(original.getUuid(), processed.getUuid());

        Assert.assertEquals(1, processed.getPayload().count());
        HerculesProtocolAssert.assertEquals(Variant.ofString("Abc ЕЁЮ"), processed.getPayload().get(TinyString.of("string-tag")));

        Assert.assertArrayEquals(original.getBytes(), processed.getBytes());
    }
}
