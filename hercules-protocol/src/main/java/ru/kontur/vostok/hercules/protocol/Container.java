package ru.kontur.vostok.hercules.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container is simple read-only wrapper for {@link Map} of tags.
 * <p>
 * Container should be immutable.
 *
 * @author Gregory Koshelev
 */
public class Container {
    private static int SIZE_OF_TAG_COUNT = Type.BYTE.size * 2;

    private final Map<TinyString, Variant> tags;
    private final int size;

    private Container(Map<TinyString, Variant> tags, int size) {
        this.tags = tags;
        this.size = size;
    }

    public Variant get(TinyString tag) {
        return tags.get(tag);
    }

    /**
     * Return tags.
     * <p>
     * Method returns underlying {@link Map} to avoid extra allocations.
     * Be careful to preserve immutability of the Container.
     *
     * @return tags
     */
    public Map<TinyString, Variant> tags() {
        return tags;
    }

    /**
     * Return tags count.
     *
     * @return tags count
     */
    public int count() {
        return tags.size();
    }

    /**
     * Return size of Container in bytes.
     *
     * @return size in bytes
     */
    public int sizeOf() {
        return size;
    }

    public static Container of(TinyString tag, Variant value) {
        return new Container(
                Collections.singletonMap(tag, value),
                SIZE_OF_TAG_COUNT + tag.sizeOf() + value.sizeOf());
    }

    public static Container of(String tag, Variant value) {
        return Container.of(TinyString.of(tag), value);
    }

    public static Container of(Map<TinyString, Variant> tags) {
        int size = SIZE_OF_TAG_COUNT;
        for (Map.Entry<TinyString, Variant> tag : tags.entrySet()) {
            size += tag.getKey().sizeOf();
            size += tag.getValue().sizeOf();
        }
        return new Container(tags, size);
    }

    public static Container empty() {
        return new Container(Collections.emptyMap(), SIZE_OF_TAG_COUNT);
    }

    public static ContainerBuilder builder() {
        return new ContainerBuilder();
    }

    public static class ContainerBuilder {
        private final Map<TinyString, Variant> tags = new LinkedHashMap<>();
        private int size = SIZE_OF_TAG_COUNT;

        private ContainerBuilder() {

        }

        public ContainerBuilder tag(String key, Variant value) {
            return tag(TinyString.of(key), value);
        }

        public ContainerBuilder tag(TinyString key, Variant value) {
            Variant oldValue = tags.put(key, value);

            if (oldValue == null) {
                size += key.sizeOf();
            } else {
                size -= oldValue.sizeOf();
            }
            size += value.sizeOf();

            return this;
        }


        /**
         * Add all tags.
         * <p>
         * Effective equivalent of {@code tags.forEach(builder::tag)}.
         *
         * @param tags tags to add
         * @return this builder
         */
        public ContainerBuilder tags(Map<TinyString, Variant> tags) {
            tags.forEach(this::tag);

            return this;
        }

        public Container build() {
            return new Container(tags, size);
        }
    }

}
