package ru.kontur.vostok.hercules.graphite.adapter.server;

import com.fasterxml.jackson.databind.exc.InvalidNullException;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.graphite.adapter.metric.MetricTag;
import ru.kontur.vostok.hercules.util.text.AsciiUtil;

/**
 * The metric reader reads metric from a buffer.
 * <p>
 * A buffer provides a single metric.
 * See handler pipeline in the {@link GraphiteAdapterServer} for details.
 *
 * @author Gregory Koshelev
 * @see GraphiteAdapterServer
 * @see GraphiteHandler
 */
public final class MetricReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricReader.class);

    /**
     * Read {@link Metric} from the buffer.
     *
     * @param buf the buffer
     * @return a metric or {@code null} in case of any errors
     */
    public static Metric read(ByteBuf buf) {
        try {
            int length = buf.bytesBefore(AsciiUtil.ASCII_SPACE);
            if (length == -1) {
                skipReadableBytes(buf);
                return null;
            }

            int lengthToSemicolon = buf.bytesBefore(AsciiUtil.ASCII_SEMICOLON);
            boolean hasTags = (lengthToSemicolon != -1);

            byte[] metricName = readStringAsBytes(buf, hasTags ? lengthToSemicolon : length);

            MetricTag[] tags = hasTags ? readTags(buf, length - lengthToSemicolon) : null;

            if (!hasMoreBytes(buf)) {
                return null;
            }
            buf.skipBytes(1);// Skip space between metric name and value

            length = buf.bytesBefore(AsciiUtil.ASCII_SPACE);
            if (length == -1) {
                skipReadableBytes(buf);
                return null;
            }

            double metricValue;
            try {
                metricValue = readDouble(buf, length);
            } catch (NumberFormatException ex) {
                skipReadableBytes(buf);
                return null;
            }
            //TODO: Ignore metrics with NaN value

            if (!hasMoreBytes(buf)) {
                return null;
            }
            buf.skipBytes(1);// Skip space between metric value and timestamp

            long metricTimestamp;
            try {
                metricTimestamp = readLong(buf, buf.readableBytes());
            } catch (NumberFormatException ex) {
                return null;
            }

            return new Metric(metricName, tags, metricValue, metricTimestamp);
        } catch (Exception ex) {
            LOGGER.warn("Got exception", ex);
            return null;
        }
    }

    private static double readDouble(ByteBuf buf, int length) {
        return Double.parseDouble(buf.readCharSequence(length, CharsetUtil.UTF_8).toString());
    }

    private static long readLong(ByteBuf buf, int length) {
        return Long.parseLong(buf.readCharSequence(length, CharsetUtil.UTF_8).toString());
    }

    private static byte[] readStringAsBytes(ByteBuf buf, int length) {
        byte[] value = new byte[length];
        buf.readBytes(value);
        return value;
    }

    private static MetricTag[] readTags(ByteBuf buf, int length) {
        int count = countBytes(buf, length, AsciiUtil.ASCII_EQUAL_SIGN);
        if (count == 0) {
            return null;
        }
        int bytesLeft = length;
        MetricTag[] tags = new MetricTag[count];
        for (int i = 0; i < count; i++) {
            byte b = buf.readByte();
            bytesLeft--;
            if (b != AsciiUtil.ASCII_SEMICOLON) {
                buf.skipBytes(bytesLeft);
                return null;
            }

            int keyLength = buf.bytesBefore(bytesLeft, AsciiUtil.ASCII_EQUAL_SIGN);
            if (keyLength == -1) {
                buf.skipBytes(bytesLeft);
                return null;
            }

            byte[] key = new byte[keyLength];
            buf.readBytes(key);
            bytesLeft -= keyLength;

            buf.skipBytes(1);// Skip '='
            bytesLeft--;

            int valueLength = buf.bytesBefore(bytesLeft, AsciiUtil.ASCII_SEMICOLON);
            if (valueLength == -1) {
                valueLength = bytesLeft;
            }

            byte[] value = new byte[valueLength];
            buf.readBytes(value);
            bytesLeft -= valueLength;

            tags[i] = new MetricTag(key, value);
        }

        return tags;
    }

    private static boolean hasMoreBytes(ByteBuf buf) {
        return buf.readableBytes() > 0;
    }

    private static int countBytes(ByteBuf buf, int length, byte value) {
        int count = 0;
        int fromIndex = buf.readerIndex();
        int toIndex = fromIndex + length;
        do {
            int index = buf.indexOf(fromIndex, toIndex, value);
            if (index == -1) {
                return count;
            }
            fromIndex = index + 1;
            count++;
        } while (fromIndex < toIndex);
        return count;
    }

    private static void skipReadableBytes(ByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }

    private MetricReader() {
        /* static class */
    }
}
