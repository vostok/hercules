package ru.kontur.vostok.hercules.json.mapping;

import ru.kontur.vostok.hercules.json.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Map;

/**
 * Maps tag preserving its name using {@link Transformer#PLAIN}.
 *
 * @author Gregory Koshelev
 */
public class PlainMapper implements Mapper {
    @Override
    public void map(TinyString tag, Variant value, Map<String, Object> document) {
        document.putIfAbsent(tag.toString(), Transformer.PLAIN.transform(value));
    }
}
