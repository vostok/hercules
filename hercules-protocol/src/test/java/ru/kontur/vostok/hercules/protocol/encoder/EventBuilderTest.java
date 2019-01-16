package ru.kontur.vostok.hercules.protocol.encoder;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

public class EventBuilderTest {

    @Test
    public void shouldBuildCorrectEvent() throws Exception {
        UUID uuid = UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000");
        EventBuilder eventBuilder = EventBuilder.create(TimeUtil.gregorianToUnixTicks(uuid.timestamp()), uuid)
            .tag("host", Variant.ofString("localhost"))
            .tag("timestamp", Variant.ofLong(1527679920000000L));

        Event event = eventBuilder.build();

        byte[] data = loadBytesFrom("v1.event.1.txt");
        data = Arrays.copyOfRange(data, 4, data.length); // Cut record count = 1 (int value)

        Assert.assertArrayEquals(data, event.getBytes());
    }

    private byte[] loadBytesFrom(String resourcePath) throws Exception {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        Path path = Paths.get(resource.toURI());
        return Files.readAllBytes(path);
    }
}
