package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.util.List;

/**
 * Variant converter is used to convert OpenTelemetry AnyValue to Hercules Variant.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/common/v1/common.proto">
 * OpenTelemetry common.proto AnyValue</a>
 */
public class VariantConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(VariantConverter.class);

    public static Variant convert(AnyValue value) {
        switch (value.getValueCase()) {
            case INT_VALUE:
                return Variant.ofLong(value.getIntValue());
            case DOUBLE_VALUE:
                return Variant.ofDouble(value.getDoubleValue());
            case STRING_VALUE:
                return Variant.ofString(value.getStringValue());
            case BOOL_VALUE:
                return Variant.ofFlag(value.getBoolValue());
            case ARRAY_VALUE:
                return Variant.ofVector(getVector(value.getArrayValue()));
            case KVLIST_VALUE:
                return Variant.ofContainer(getContainer(value.getKvlistValue().getValuesList()));
            case VALUE_NOT_SET:
                return Variant.ofNull();
            default:
                throw new IllegalStateException("Unexpected value: " + value.getValueCase());
        }
    }

    private static Container getContainer(List<KeyValue> keyValueList) {
        Container.ContainerBuilder builder = Container.builder();

        for (KeyValue keyValue : keyValueList) {
            builder.tag(keyValue.getKey(), convert(keyValue.getValue()));
        }

        return builder.build();
    }

    @NotNull
    private static Vector getVector(ArrayValue arrayValue) {
        List<AnyValue> list = arrayValue.getValuesList();
        if (list.isEmpty()) {
            return Vector.ofStrings();
        }

        AnyValue.ValueCase firstValueCase = list.get(0).getValueCase();
        if (list.stream().allMatch(it -> it.getValueCase().equals(firstValueCase))) {
            switch (firstValueCase) {
                case INT_VALUE:
                    long[] longs = new long[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        longs[i] = list.get(i).getIntValue();
                    }
                    return Vector.ofLongs(longs);
                case DOUBLE_VALUE:
                    double[] doubles = new double[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        doubles[i] = list.get(i).getDoubleValue();
                    }
                    return Vector.ofDoubles(doubles);
                case STRING_VALUE:
                    String[] strings = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        strings[i] = list.get(i).getStringValue();
                    }
                    return Vector.ofStrings(strings);
                case BOOL_VALUE:
                    boolean[] booleans = new boolean[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        booleans[i] = list.get(i).getBoolValue();
                    }
                    return Vector.ofFlags(booleans);
                case ARRAY_VALUE:
                    Vector[] vectors = new Vector[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        vectors[i] = getVector(list.get(i).getArrayValue());
                    }
                    return Vector.ofVectors(vectors);
                case KVLIST_VALUE:
                    Container[] containers = new Container[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        containers[i] = getContainer(list.get(i).getKvlistValue().getValuesList());
                    }
                    return Vector.ofContainers(containers);
                case VALUE_NOT_SET:
                    return Vector.ofNulls();
                default:
                    throw new IllegalStateException("Unexpected firstValueCase: " + firstValueCase);
            }
        } else {
            LOGGER.error("Not support ArrayValue with not match value types");

            return Vector.ofNulls();
        }
    }
}
