package ru.kontur.vostok.hercules.elastic.sink.format;

import ru.kontur.vostok.hercules.elastic.sink.StackTraceCreator;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * @author Gregory Koshelev
 */
public class ExceptionToStackTraceTransformer implements Transformer {
    @Override
    public String transform(Variant value) {
        if (value.getType() != Type.CONTAINER) {
            return null;
        }
        return StackTraceCreator.createStackTrace((Container)value.getValue());
    }
}
