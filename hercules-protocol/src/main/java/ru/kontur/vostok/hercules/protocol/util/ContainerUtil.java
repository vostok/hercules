package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
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

        final String line = pretty ? System.lineSeparator() : "";
        final String separator = "," + line;
        final String equals = pretty ? " = " : "=";
        final String indentString = pretty ? indentString(indent + 1) : "";
        final String endIndentString = pretty ? indentString(indent) : "";

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
                case VECTOR:
                    printVectorValue(sb, (Vector) value, pretty, indent + 1);
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
                sb.append(new String((byte[]) value , StandardCharsets.UTF_8));
                break;
            default:
                throw new NotImplementedException(type);
        }
    }

    private static void printVectorValue(final StringBuilder sb, final Vector vector, final boolean pretty, final int indent) {

        final String line = pretty ? System.lineSeparator() : "";
        final String separator = "," + line;
        final String indentString = pretty ? indentString(indent + 1) : "";
        final String endIndentString = pretty ? indentString(indent) : "";

        sb.append("[").append(line);
        switch (vector.getType()) {
            case BYTE:
                for (byte b : (byte[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.BYTE, b);
                    sb.append(separator);
                }
                break;
            case SHORT:
                for (short s : (short[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.SHORT, s);
                    sb.append(separator);
                }
                break;
            case INTEGER:
                for (int i : (int[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.INTEGER, i);
                    sb.append(separator);
                }
                break;
            case LONG:
                for (long l : (long[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.LONG, l);
                    sb.append(separator);
                }
                break;
            case FLOAT:
                for (float f : (float[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.FLOAT, f);
                    sb.append(separator);
                }
                break;
            case DOUBLE:
                for (double d : (double[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.DOUBLE, d);
                    sb.append(separator);
                }
                break;
            case FLAG:
                for (boolean b : (boolean[]) vector.getValue()) {
                    sb.append(indentString);
                    printPrimitiveValue(sb, Type.FLAG, b);
                    sb.append(separator);
                }
                break;
            case CONTAINER:
                for (Container c : (Container[]) vector.getValue()) {
                    sb.append(indentString);
                    toString(sb, c, pretty, indent + 1);
                    sb.append(separator);
                }
                break;
            default:
                throw new NotImplementedException(vector.getType());
        }
        sb.append(endIndentString).append("]");
    }

    public static String indentString(int indent) {
        return repeat(" ", indent * INDENT_SPACES);
    }

    private static String repeat(final String s, int count) {
        return new String(new char[count]).replace("\0", s);
    }

    private ContainerUtil() {
        /* static class */
    }
}
