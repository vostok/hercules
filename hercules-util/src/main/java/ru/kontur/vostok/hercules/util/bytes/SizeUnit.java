package ru.kontur.vostok.hercules.util.bytes;

/**
 * @author Gregory Koshelev
 */
public enum SizeUnit {
    BYTES {
        public long toBytes(long size) {
            return size;
        }

        public long toKBytes(long size) {
            return size >> 10;
        }

        public long toMBytes(long size) {
            return size >> 20;
        }

        public long toGBytes(long size) {
            return size >> 30;
        }

        public long convert(long sourceSize, SizeUnit sourceUnit) {
            return sourceUnit.toBytes(sourceSize);
        }
    },

    KILOBYTES {
        public long toBytes(long size) {
            return size << 10;
        }

        public long toKBytes(long size) {
            return size;
        }

        public long toMBytes(long size) {
            return size >> 10;
        }

        public long toGBytes(long size) {
            return size >> 20;
        }

        public long convert(long sourceSize, SizeUnit sourceUnit) {
            return sourceUnit.toKBytes(sourceSize);
        }
    },

    MEGABYTES {
        public long toBytes(long size) {
            return size << 20;
        }

        public long toKBytes(long size) {
            return size << 10;
        }

        public long toMBytes(long size) {
            return size;
        }

        public long toGBytes(long size) {
            return size >> 10;
        }

        public long convert(long sourceSize, SizeUnit sourceUnit) {
            return sourceUnit.toMBytes(sourceSize);
        }
    },

    GIGABYTES {
        public long toBytes(long size) {
            return size << 30;
        }

        public long toKBytes(long size) {
            return size << 20;
        }

        public long toMBytes(long size) {
            return size << 10;
        }

        public long toGBytes(long size) {
            return size;
        }

        public long convert(long sourceSize, SizeUnit sourceUnit) {
            return sourceUnit.toGBytes(sourceSize);
        }
    };

    public long toBytes(long size) {
        throw new AbstractMethodError();
    }

    public long toKBytes(long size) {
        throw new AbstractMethodError();
    }

    public long toMBytes(long size) {
        throw new AbstractMethodError();
    }

    public long toGBytes(long size) {
        throw new AbstractMethodError();
    }

    public long convert(long sourceSize, SizeUnit sourceUnit) {
        throw new AbstractMethodError();
    }
}
