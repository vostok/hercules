package ru.kontur.vostok.hercules.protocol.format;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class EventFormatter {
    private static final int INDENT_SPACES = 2;

    /**
     * Format event into single-line string or pretty multi-line string (depends on {@code pretty} parameter)
     *
     * @param event
     * @param pretty
     * @return formatted event
     */
    public static String format(final Event event, final boolean pretty) {
        final StringBuilder sb = new StringBuilder(event.getBytes().length * 2);

        final String line = pretty ? System.lineSeparator() : "";
        final String separator = "," + line;
        final String equals = pretty ? " = " : "=";
        final String indentString = pretty ? indentString(1) : "";

        sb.append("{").append(line);
        sb.append(indentString).append("version").append(equals).append(event.getVersion()).append(separator);
        sb.append(indentString).append("timestamp").append(equals).append(event.getTimestamp()).append(separator);
        sb.append(indentString).append("uuid").append(equals).append(event.getUuid()).append(separator);
        sb.append(indentString).append("payload").append(equals);
        appendContainer(sb, event.getPayload(), pretty, 1);
        sb.append(separator);
        sb.append("}");

        return sb.toString();
    }

    /**
     * Format container into single-line string or pretty multi-line string (depends on {@code pretty} parameter)
     *
     * @param container
     * @param pretty
     * @return formatted container
     */
    public static String format(final Container container, final boolean pretty) {
        final StringBuilder sb = new StringBuilder();
        appendContainer(sb, container, pretty, 0);
        return sb.toString();
    }

    private static void appendContainer(final StringBuilder sb, final Container container, final boolean pretty, final int indent) {
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
                    appendContainer(sb, (Container) value, pretty, indent + 1);
                    break;
                case VECTOR:
                    appendVector(sb, (Vector) value, pretty, indent + 1);
                    break;
                default:
                    appendScalar(sb, type, value);
                    break;
            }

            sb.append(separator);
        }
        sb.append(endIndentString).append("}");
    }

    private static void appendScalar(final StringBuilder sb, final Type type, final Object value) {
        switch (type) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLAG:
            case FLOAT:
            case DOUBLE:
            case UUID:
                sb.append(value);
                break;
            case STRING:
                sb.append(new String((byte[]) value, StandardCharsets.UTF_8));
                break;
            case NULL:
                sb.append("null");
                break;
            default:
                throw new NotImplementedException(type);
        }
    }

    private static void appendVector(final StringBuilder sb, final Vector vector, final boolean pretty, final int indent) {

        final String line = pretty ? System.lineSeparator() : "";
        final String separator = "," + line;
        final String indentString = pretty ? indentString(indent + 1) : "";
        final String endIndentString = pretty ? indentString(indent) : "";

        sb.append("[").append(line);
        switch (vector.getType()) {
            case CONTAINER:
                for (Container c : (Container[]) vector.getValue()) {
                    sb.append(indentString);
                    appendContainer(sb, c, pretty, indent + 1);
                    sb.append(separator);
                }
                break;
            case BYTE:
                for (byte b : (byte[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.BYTE, b);
                    sb.append(separator);
                }
                break;
            case SHORT:
                for (short s : (short[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.SHORT, s);
                    sb.append(separator);
                }
                break;
            case INTEGER:
                for (int i : (int[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.INTEGER, i);
                    sb.append(separator);
                }
                break;
            case LONG:
                for (long l : (long[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.LONG, l);
                    sb.append(separator);
                }
                break;
            case FLAG:
                for (boolean b : (boolean[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.FLAG, b);
                    sb.append(separator);
                }
                break;
            case FLOAT:
                for (float f : (float[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.FLOAT, f);
                    sb.append(separator);
                }
                break;
            case DOUBLE:
                for (double d : (double[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.DOUBLE, d);
                    sb.append(separator);
                }
                break;
            case STRING:
                for (byte[] string : (byte[][]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.STRING, string);
                    sb.append(separator);
                }
                break;
            case UUID:
                for (UUID uuid : (UUID[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.UUID, uuid);
                    sb.append(separator);
                }
            case NULL:
                for (Object ignored : (Object[]) vector.getValue()) {
                    sb.append(indentString);
                    appendScalar(sb, Type.NULL, null);
                    sb.append(separator);
                }
                break;
            default:
                throw new NotImplementedException(vector.getType());
        }
        sb.append(endIndentString).append("]");
    }

    private static String indentString(int indent) {
        return StringUtil.repeat(' ', indent * INDENT_SPACES);
    }
}
