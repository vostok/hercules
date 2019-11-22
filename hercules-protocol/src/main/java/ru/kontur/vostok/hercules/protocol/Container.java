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
                Sizes.sizeOfTagCount() + Sizes.sizeOfTinyString(tag) + Sizes.sizeOfVariant(value));
    }

    public static Container of(String tag, Variant value) {
        return Container.of(TinyString.of(tag), value);
    }

    public static Container of(Map<TinyString, Variant> tags) {
        int size = Sizes.sizeOfTagCount();
        for (Map.Entry<TinyString, Variant> tag : tags.entrySet()) {
            size += Sizes.sizeOfTinyString(tag.getKey());
            size += Sizes.sizeOfVariant(tag.getValue());
        }
        return new Container(tags, size);
    }

    public static Container empty() {
        return new Container(Collections.emptyMap(), Sizes.sizeOfTagCount());
    }

    public static ContainerBuilder builder() {
        return new ContainerBuilder();
    }

    public static class ContainerBuilder {
        private final Map<TinyString, Variant> tags = new LinkedHashMap<>();
        private int size = Sizes.sizeOfTagCount();

        private ContainerBuilder() {

        }

        public ContainerBuilder tag(String key, Variant value) {
            return tag(TinyString.of(key), value);
        }

        public ContainerBuilder tag(TinyString key, Variant value) {
            tags.put(key, value);
            size += Sizes.sizeOfTinyString(key);
            size += Sizes.sizeOfVariant(value);

            return this;
        }

        public Container build() {
            return new Container(tags, size);
        }
    }

}
