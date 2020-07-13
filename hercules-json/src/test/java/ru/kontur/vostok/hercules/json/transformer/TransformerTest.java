package ru.kontur.vostok.hercules.json.transformer;

import org.junit.Test;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.transformer.PlainTransformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class TransformerTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testContainerPlainTransformation() throws IOException {
        Container container = Container.builder().
                tag("Byte", Variant.ofByte((byte) 127)).
                tag("Short", Variant.ofShort((short) 10_000)).
                tag("Int", Variant.ofInteger(123_456_789)).
                tag("Long", Variant.ofLong(123_456_789L)).
                tag("Float", Variant.ofFloat(0.123456f)).
                tag("Double", Variant.ofDouble(0.123456)).
                tag("Flag of true", Variant.ofFlag(true)).
                tag("Flag of false", Variant.ofFlag(false)).
                tag("String", Variant.ofString("abc")).
                tag("Json String", Variant.ofString("String with json inside {\"a\": {\"b\": [123, true, \"str\"]}}")).
                tag("Array of Integers", Variant.ofVector(Vector.ofIntegers(1, 2, 3))).
                tag("Internal Container", Variant.ofContainer(Container.of("a", Variant.ofString("a")))).
                tag("Vector of Containers", Variant.ofVector(Vector.ofContainers(Container.of("1", Variant.ofInteger(1)), Container.of("2", Variant.ofInteger(2))))).
                tag("Vector of Vectors", Variant.ofVector(Vector.ofVectors(Vector.ofIntegers(1, 2, 3), Vector.ofStrings("a", "b", "c")))).
                build();

        Object transform = PlainTransformer.PLAIN.transform(Variant.ofContainer(container));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DocumentWriter.writeTo(stream, (Map<String, Object>) transform);

        assertEquals(
                "{" +
                        "\"Byte\":127," +
                        "\"Short\":10000," +
                        "\"Int\":123456789," +
                        "\"Long\":123456789," +
                        "\"Float\":0.123456," +
                        "\"Double\":0.123456," +
                        "\"Flag of true\":true," +
                        "\"Flag of false\":false," +
                        "\"String\":\"abc\"," +
                        "\"Json String\":\"String with json inside {\\\"a\\\": {\\\"b\\\": [123, true, \\\"str\\\"]}}\"," +
                        "\"Array of Integers\":[1,2,3]," +
                        "\"Internal Container\":{\"a\":\"a\"}," +
                        "\"Vector of Containers\":[{\"1\":1},{\"2\":2}]," +
                        "\"Vector of Vectors\":[[1,2,3],[\"a\",\"b\",\"c\"]]" +
                        "}",
                stream.toString(StandardCharsets.UTF_8.name()));
    }
}
