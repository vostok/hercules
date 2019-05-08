package ru.kontur.vostok.hercules.partitioner;

/**
 * @author Gregory Koshelev
 */
public class ShardingKey {
    private static final ShardingKey EMPTY = new ShardingKey(new String[0][]);

    private final String[][] keys;

    private ShardingKey(String[][] keys) {
        this.keys = keys;
    }

    public static ShardingKey fromKeyPaths(String... keyPaths) {
        if (keyPaths == null || keyPaths.length == 0) {
            return empty();
        }

        int size = keyPaths.length;

        String[][] keys = new String[size][];
        for (int i = 0; i < size; i++) {
            keys[i] = keyPaths[i].split("/");
        }

        return new ShardingKey(keys);
    }

    public static ShardingKey fromTag(String tag) {
        return new ShardingKey(new String[][]{{tag}});
    }

    public static ShardingKey fromTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return empty();
        }

        int size = tags.length;

        String[][] keys = new String[size][1];
        for (int i = 0; i < size; i++) {
            keys[i][0] = tags[i];
        }

        return new ShardingKey(keys);
    }

    public static ShardingKey empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public String[][] getKeys() {
        return keys;
    }
}
