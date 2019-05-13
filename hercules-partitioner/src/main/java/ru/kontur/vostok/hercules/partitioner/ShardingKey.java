package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.hpath.HPath;

/**
 * @author Gregory Koshelev
 */
public class ShardingKey {
    private static final ShardingKey EMPTY = new ShardingKey(new HPath[0]);

    private final HPath[] keys;

    private ShardingKey(HPath[] keys) {
        this.keys = keys;
    }

    public static ShardingKey fromKeyPaths(String... keyPaths) {
        if (keyPaths == null || keyPaths.length == 0) {
            return empty();
        }

        int size = keyPaths.length;

        HPath[] keys = new HPath[size];
        for (int i = 0; i < size; i++) {
            keys[i] = new HPath(keyPaths[i]);
        }

        return new ShardingKey(keys);
    }

    public static ShardingKey fromTag(String tag) {
        return new ShardingKey(new HPath[]{new HPath(tag)});
    }

    public static ShardingKey fromTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return empty();
        }

        int size = tags.length;

        HPath[] keys = new HPath[size];
        for (int i = 0; i < size; i++) {
            keys[i] = HPath.fromTag(tags[i]);
        }

        return new ShardingKey(keys);
    }

    public static ShardingKey empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public HPath[] getKeys() {
        return keys;
    }

    public int size() {
        return keys != null ? keys.length : 0;
    }
}
