package ru.kontur.vostok.hercules.protocol.hpath;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Gregory Koshelev
 */
public class HPath {
    private final String path;

    private final String[] tags;

    private HPath(String path, String[] tags) {
        this.path = path;
        this.tags = tags;
    }

    public Variant extract(Container container) {
        if (tags.length == 0) {
            return null;
        }

        int size = tags.length;
        Container current = container;

        for (int i = 0; i < size - 1; i++) {
            String tag = tags[i];
            Variant tagValue = current.get(tag);
            if (tagValue == null || tagValue.getType() != Type.CONTAINER) {
                return null;
            }
            current = (Container) tagValue.getValue();
        }

        return current.get(tags[size - 1]);
    }

    public String getRootTag() {
        return (tags.length > 0) ? tags[0] : null;
    }

    public HPath getSubHPath() {
        if (tags.length == 0) {
            return this;
        }

        String[] subTags = new String[tags.length - 1];
        System.arraycopy(tags, 1, subTags, 0, tags.length - 1);

        return fromTags(subTags);
    }

    public String getPath() {
        return path;
    }

    public TagIterator it() {
        return new TagIterator();
    }

    private static String[] pathToTags(String path) {
        return (path != null) ? path.split("/") : new String[0];
    }

    private static String tagsToPath(String... tags) {
        return String.join("/", tags);
    }

    public static HPath fromTag(String tag) {
        return new HPath(tag, new String[]{tag});
    }

    public static HPath fromTags(String... tags) {
        return new HPath(tagsToPath(tags), tags);
    }

    public static HPath fromPath(String path) {
        return new HPath(path, pathToTags(path));
    }

    /**
     * Iterate over tags in HPath.
     * <p>
     * It is not thread-safe.
     */
    public class TagIterator implements Iterator<String> {
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < tags.length;
        }

        @Override
        public String next() {
            int i = cursor;
            if (i >= tags.length) {
                throw new NoSuchElementException();
            }
            cursor = i + 1;
            return tags[i];
        }
    }
}
