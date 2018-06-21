package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader2;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;

import java.util.Collections;

public class EventReader2WriteReadTest {

    @Test
    public void shouldWriteReadAllTags() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader2.readAllTags());

        EventBuilder builder = new EventBuilder();
        builder.setVersion(1);
        builder.setTimestamp(123_456_789L);
        builder.setTag("string-tag", Variant.ofString("Abc ЕЁЮ"));
        builder.setTag("flag-array-tag", Variant.ofFlagArray(new boolean[]{true, true, false}));

        pipe.process(builder.build()).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadNoTags() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader2.readNoTags());

        EventBuilder builder = new EventBuilder();
        builder.setVersion(1);
        builder.setTimestamp(123_456_789L);

        builder.setTag("string-tag", Variant.ofString("Abc ЕЁЮ"));
        builder.setTag("flag-array-tag", Variant.ofFlagArray(new boolean[]{true, true, false}));

        WriteReadPipe.ProcessedCapture<Event> capture = pipe.process(builder.build());

        Event processed = capture.getProcessed();
        Event original = capture.getOriginal();

        Assert.assertEquals(original.getVersion(), processed.getVersion());
        Assert.assertEquals(original.getTimestamp(), processed.getTimestamp());

        Assert.assertEquals(0, processed.getTags().size());

        Assert.assertArrayEquals(original.getBytes(), processed.getBytes());
    }

    @Test
    public void shouldWriteReadOneTag() {
        WriteReadPipe<Event> pipe = WriteReadPipe.init(new EventWriter(), EventReader2.readTags(Collections.singleton("string-tag")));

        EventBuilder builder = new EventBuilder();
        builder.setVersion(1);
        builder.setTimestamp(123_456_789L);

        builder.setTag("string-tag", Variant.ofString("Abc ЕЁЮ"));
        builder.setTag("flag-array-tag", Variant.ofFlagArray(new boolean[]{true, true, false}));

        WriteReadPipe.ProcessedCapture<Event> capture = pipe.process(builder.build());

        Event processed = capture.getProcessed();
        Event original = capture.getOriginal();

        Assert.assertEquals(original.getVersion(), processed.getVersion());
        Assert.assertEquals(original.getTimestamp(), processed.getTimestamp());

        Assert.assertEquals(1, processed.getTags().size());
        HerculesProtocolAssert.assertEquals(Variant.ofString("Abc ЕЁЮ"), processed.getTags().get("string-tag"));

        Assert.assertArrayEquals(original.getBytes(), processed.getBytes());
    }
}
