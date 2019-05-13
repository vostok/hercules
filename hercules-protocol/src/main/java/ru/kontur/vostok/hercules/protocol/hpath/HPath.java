package ru.kontur.vostok.hercules.protocol.hpath;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * @author Gregory Koshelev
 */
public class HPath {
    private final String path;

    private final String[] tags;

    public HPath(String path) {
        this.path = path;

        tags = pathToTags(path);
    }

    public Variant extract(Container container) {
        if (tags == null || tags.length == 0) {
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
        return (tags != null && tags.length > 0) ? tags[0] : null;
    }

    public String getPath() {
        return path;
    }

    private static String[] pathToTags(String path) {
        return (path != null) ? path.split("/") : new String[0];
    }

    public static HPath fromTag(String tag) {
        return new HPath(tag);
    }
}
