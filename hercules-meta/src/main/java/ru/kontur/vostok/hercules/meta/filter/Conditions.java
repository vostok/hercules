package ru.kontur.vostok.hercules.meta.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.kontur.vostok.hercules.protocol.TagValue;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public class Conditions {

    public static class Negate implements Condition {
        private Condition condition;

        public Condition getCondition() {
            return condition;
        }

        public void setCondition(Condition condition) {
            this.condition = condition;
        }

        @Override
        public boolean test(TagValue tagValue) {
            return !condition.test(tagValue);
        }
    }

    public static class Range implements Condition {
        private long left;
        private long right;
        private boolean inclusiveLeft;
        private boolean inclusiveRight;

        public long getLeft() {
            return left;
        }

        public void setLeft(long left) {
            this.left = left;
        }

        public long getRight() {
            return right;
        }

        public void setRight(long right) {
            this.right = right;
        }

        public boolean isInclusiveLeft() {
            return inclusiveLeft;
        }

        public void setInclusiveLeft(boolean inclusiveLeft) {
            this.inclusiveLeft = inclusiveLeft;
        }

        public boolean isInclusiveRight() {
            return inclusiveRight;
        }

        public void setInclusiveRight(boolean inclusiveRight) {
            this.inclusiveRight = inclusiveRight;
        }

        @Override
        public boolean test(TagValue tagValue) {
           if (!isAssignableToLong(tagValue)) {
               return false;
           }
           long value = toLong(tagValue);
           return (inclusiveLeft ? left <= value : left < value) && (inclusiveRight ? right >= value : right > value);
        }
    }

    public static class NumericalEquality implements Condition {
        private long value;

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        @Override
        public boolean test(TagValue tagValue) {
            return isAssignableToLong(tagValue) && toLong(tagValue) == value;
        }
    }

    public static class StringEquality implements Condition {
        private String value;
        @JsonIgnore
        private byte[] bytes;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            this.bytes = value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public boolean test(TagValue tagValue) {
            Type type = tagValue.getType();
            return (type == Type.STRING || type == Type.TEXT) && Arrays.equals(bytes, (byte[]) tagValue.getValue());
        }
    }

    public static class StartsWith implements Condition {
        private String value;
        @JsonIgnore
        private byte[] bytes;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            this.bytes = value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public boolean test(TagValue tagValue) {
            Type type = tagValue.getType();
            return (type == Type.STRING || type == Type.TEXT) && ByteUtil.isSubarray((byte[]) tagValue.getValue(), bytes);
        }
    }

    private static boolean isAssignableToLong(TagValue tagValue) {
        Type type = tagValue.getType();
        return type == Type.BYTE || type == Type.SHORT || type == Type.INTEGER || type == Type.LONG;
    }

    private static long toLong(TagValue value) {
        return (long) value.getValue();
    }
}
