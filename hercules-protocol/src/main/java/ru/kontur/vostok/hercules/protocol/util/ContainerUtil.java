package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * ContainerUtil
 *
 * @author Kirill Sulim
 */
public class ContainerUtil {

    private static final int INDENT_SPACES = 2;

    public static <T> T extract(Container container, TagDescription<T> tag) {
        Variant variant = container.get(tag.getName());
        Type type = Optional.ofNullable(variant).map(Variant::getType).orElse(null);
        Function<Object, ? extends T> extractor = tag.getExtractors().get(type);
        if (Objects.isNull(extractor)) {
            throw new IllegalArgumentException(String.format("Tag '%s' cannot contain value of type '%s'", tag.getName(),  type));
        } else {
            Object value = Optional.ofNullable(variant).map(Variant::getValue).orElse(null);
            return extractor.apply(value);
        }
    }

    public static String toString(final Container container, final boolean pretty) {
        final StringBuilder sb = new StringBuilder();
        toString(sb, container, pretty, 0);
        return sb.toString();
    }

    public static void toString(final StringBuilder sb, final Container container, final boolean pretty, final int indent) {

        final String line = pretty ? "\n" : "";
        final String separator = "," + line;
        final String equals = pretty ? " = " : "=";
        final String indentString = pretty ? multiply(" ", (indent + 1) * INDENT_SPACES) : "";
        final String endIndentString = pretty ? multiply(" ", indent * INDENT_SPACES) : "";

        sb.append("{").append(line);
        for (Map.Entry<String, Variant> variantEntry : container) {
            final String tagName = variantEntry.getKey();
            final Type type = variantEntry.getValue().getType();
            final Object value = variantEntry.getValue().getValue();

            sb.append(indentString).append(tagName).append(equals);
            switch (type) {
                case CONTAINER:
                    toString(sb, (Container) value, pretty, indent + 1);
                    break;
                case BYTE_ARRAY:
                case BYTE_VECTOR:
                case SHORT_ARRAY:
                case SHORT_VECTOR:
                case INTEGER_ARRAY:
                case INTEGER_VECTOR:
                case LONG_ARRAY:
                case LONG_VECTOR:
                case FLOAT_ARRAY:
                case FLOAT_VECTOR:
                case DOUBLE_ARRAY:
                case DOUBLE_VECTOR:
                case FLAG_ARRAY:
                case FLAG_VECTOR:
                case CONTAINER_ARRAY:
                case CONTAINER_VECTOR:
                    printArrayValue(sb, type, value, pretty, indent + 1);
                    break;
                default:
                    printPrimitiveValue(sb, type, value);
                    break;
            }

            sb.append(separator);
        }
        sb.append(endIndentString).append("}");
    }

    private static void printPrimitiveValue(final StringBuilder sb, final Type type, final Object value) {
        switch (type) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case FLAG:
                sb.append(value);
                break;
            case STRING:
            case TEXT:
                sb.append(new String((byte[]) value , StandardCharsets.UTF_8));
                break;
            default:
                throw new NotImplementedException(type);
        }
    }

    private static void printArrayValue(final StringBuilder sb, final Type arrayType, final Object value, final boolean pretty, final int indent) {

        final String line = pretty ? "\n" : "";
        final String separator = "," + line;
        final String indentString = pretty ? multiply(" ", (indent + 1) * INDENT_SPACES) : "";
        final String endIndentString = pretty ? multiply(" ", indent * INDENT_SPACES) : "";

        sb.append("[").append(line);
        switch (arrayType) {
            case BYTE_ARRAY:
            case BYTE_VECTOR:
                for (byte b : (byte[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.BYTE, b);
                    sb.append(separator);
                }
                break;
            case SHORT_ARRAY:
            case SHORT_VECTOR:
                for (short s : (short[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.SHORT, s);
                    sb.append(separator);
                }
                break;
            case INTEGER_ARRAY:
            case INTEGER_VECTOR:
                for (int i : (int[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.INTEGER, i);
                    sb.append(separator);
                }
                break;
            case LONG_ARRAY:
            case LONG_VECTOR:
                for (long l : (long[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.LONG, l);
                    sb.append(separator);
                }
                break;
            case FLOAT_ARRAY:
            case FLOAT_VECTOR:
                for (float f : (float[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.FLOAT, f);
                    sb.append(separator);
                }
                break;
            case DOUBLE_ARRAY:
            case DOUBLE_VECTOR:
                for (double d : (double[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.DOUBLE, d);
                    sb.append(separator);
                }
                break;
            case FLAG_ARRAY:
            case FLAG_VECTOR:
                for (boolean b : (boolean[]) value) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.FLAG, b);
                    sb.append(separator);
                }
                break;
            case CONTAINER_ARRAY:
            case CONTAINER_VECTOR:
                for (Container c : (Container[]) value) {
                    sb.append(indentString);
                    toString(sb, c, pretty, indent + 1);
                    sb.append(separator);
                }
                break;
            default:
                throw new NotImplementedException(arrayType);
        }
        sb.append(endIndentString).append("]");
    }

    private static String multiply(final String s, int count) {
        return new String(new char[count]).replace("\0", s);
    }

    private ContainerUtil() {
        /* static class */
    }
}
