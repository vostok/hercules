package ru.kontur.vostok.hercules.protocol.util;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TagDescriptionBuilder
 *
 * @author Kirill Sulim
 */
public class TagDescriptionBuilder<T> {

    private final String tagName;
    private final Map<Type, Function<Object, ? extends T>> extractors = new HashMap<>();

    public TagDescriptionBuilder(String tagName) {
        this.tagName = tagName;
    }

    public static <T> TagDescriptionBuilder<T> tag(String name, Class<T> clazz) {
        return new TagDescriptionBuilder<>(name);
    }

    public static TagDescriptionBuilder<String> textual(String name) {
        return new TagDescriptionBuilder<String>(name)
                .addExtractor(Type.STRING, StandardExtractors::extractString)
                .addExtractor(Type.TEXT, StandardExtractors::extractString);
    }

    public static TagDescriptionBuilder<Container[]> containerList(String name) {
        return new TagDescriptionBuilder<Container[]>(name)
                .addExtractor(Type.CONTAINER_VECTOR, StandardExtractors::extractContainerArray)
                .addExtractor(Type.CONTAINER_ARRAY, StandardExtractors::extractContainerArray);
    }

    public static <T> TagDescriptionBuilder<T> parsable(String name, Function<String, ? extends T> parser) {
        return textual(name).convert(parser);
    }

    public static <T extends Enum<T>> TagDescriptionBuilder<T> enumValue(String name, Class<T> clazz) {
        return parsable(name, s -> Enum.valueOf(clazz, s.toUpperCase()));
    }

    public static TagDescriptionBuilder<Integer> integer(String name) {
        return new TagDescriptionBuilder<Integer>(name)
                .addExtractor(Type.BYTE, o -> ((Byte) o).intValue())
                .addExtractor(Type.SHORT, o -> ((Short) o).intValue())
                .addExtractor(Type.INTEGER, o -> (Integer) o);
    }

    public TagDescriptionBuilder<T> addExtractor(Type type, Function<Object, ? extends T> extractor) {
        this.extractors.put(type, extractor);
        return this;
    }

    public TagDescriptionBuilder<T> addDefault(Supplier<? extends T> supplier) {
        this.extractors.put(null, ignore -> supplier.get());
        return this;
    }

    public <T2> TagDescriptionBuilder<T2> convert(Function<? super T, ? extends T2> converter) {
        TagDescriptionBuilder<T2> result = new TagDescriptionBuilder<>(this.tagName);
        this.extractors.forEach((type, extractor) -> result.addExtractor(type, extractor.andThen(converter)));
        return result;
    }

    public TagDescription<T> build() {
        return new TagDescription<>(tagName, extractors);
    }
}
