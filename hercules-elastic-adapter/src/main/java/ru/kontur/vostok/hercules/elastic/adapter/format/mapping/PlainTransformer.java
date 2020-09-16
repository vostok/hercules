package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.collection.ArrayUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Transforms the object to the variant as is.
 * <p>
 * Supported types:
 * <ul>
 *     <li>{@link Integer}</li>
 *     <li>{@link Long}</li>
 *     <li>{@link Float}</li>
 *     <li>{@link Double}</li>
 *     <li>{@link Boolean}</li>
 *     <li>{@link String}</li>
 *     <li>{@link Map}</li>
 *     <li>{@link List} of the types above</li>
 * </ul>
 *
 * @author Gregory Koshelev
 */
public class PlainTransformer implements Transformer {
    private static final Map<Class<?>, Function<Object, Variant>> TYPE_MAPPER;
    private static final Map<Class<?>, Function<List<?>, Variant>> LIST_TYPE_MAPPER;

    static {
        TYPE_MAPPER = new HashMap<>();
        TYPE_MAPPER.put(Integer.class, object -> Variant.ofInteger((int) object));
        TYPE_MAPPER.put(Long.class, object -> Variant.ofLong((long) object));
        TYPE_MAPPER.put(Float.class, object -> Variant.ofFloat((float) object));
        TYPE_MAPPER.put(Double.class, object -> Variant.ofDouble((double) object));
        TYPE_MAPPER.put(Boolean.class, object -> Variant.ofFlag((boolean) object));
        TYPE_MAPPER.put(String.class, object -> Variant.ofString((String) object));

        LIST_TYPE_MAPPER = new HashMap<>();
        LIST_TYPE_MAPPER.put(Integer.class, list -> Variant.ofVector(Vector.ofIntegers(list.stream().mapToInt(x -> (Integer) x).toArray())));
        LIST_TYPE_MAPPER.put(Long.class, list -> Variant.ofVector(Vector.ofLongs(list.stream().mapToLong(x -> (Long) x).toArray())));
        LIST_TYPE_MAPPER.put(Float.class, list -> Variant.ofVector(Vector.ofFloats(ArrayUtil.toFloatArray(list))));
        LIST_TYPE_MAPPER.put(Double.class, list -> Variant.ofVector(Vector.ofDoubles(list.stream().mapToDouble(x -> (Double) x).toArray())));
        LIST_TYPE_MAPPER.put(Boolean.class, list -> Variant.ofVector(Vector.ofFlags(ArrayUtil.toBooleanArray(list))));
        LIST_TYPE_MAPPER.put(String.class, list -> Variant.ofVector(Vector.ofStrings(list.toArray(new String[0]))));
        LIST_TYPE_MAPPER.put(Map.class, list -> Variant.ofVector(Vector.ofContainers(toContainersArray(list))));
    }

    @Override
    public Variant transform(Object value) {
        return toVariant(value);
    }

    /**
     * Convert object to variant.
     *
     * @param value object
     * @return variant
     * @throws InconsistentElementTypesListException if object contains list (or object is list itself) of elements with different types
     */
    private static Variant toVariant(Object value) throws InconsistentElementTypesListException {
        if (value instanceof List<?>) {
            return toVectorVariant((List<?>) value);
        }

        if (value instanceof Map<?, ?>) {
            return toContainerVariant((Map<String, Object>) value);
        }

        Class<?> clazz = value.getClass();
        Function<Object, Variant> func = TYPE_MAPPER.get(clazz);
        if (func == null) {
            throw new IllegalStateException("No mapping for class " + clazz.getSimpleName());
        }
        return func.apply(value);
    }

    /**
     * Convert map to {@link Variant} of {@link Container}.
     *
     * @param map the map
     * @return the container variant
     * @throws InconsistentElementTypesListException if internally map contains list of elements with different types
     */
    private static Variant toContainerVariant(Map<String, Object> map) throws InconsistentElementTypesListException {
        return Variant.ofContainer(toContainer(map));
    }

    /**
     * Convert list of objects to {@link Variant} of {@link Vector}
     * <p>
     * All list elements must have same type.
     *
     * @param list list of objects
     * @return {@link Variant}
     * @throws InconsistentElementTypesListException if list (or internal one) contains elements of different types
     */
    private static Variant toVectorVariant(List<?> list) throws InconsistentElementTypesListException {
        if (list.size() == 0) {
            return Variant.ofVector(Vector.ofStrings());
        }

        Object instance = list.get(0);

        Class<?> clazz = instance.getClass();
        for (Object element : list) {
            if (!element.getClass().equals(clazz)) {
                throw new InconsistentElementTypesListException();
            }
        }

        if (instance instanceof Map) {
            clazz = Map.class;
        }

        Function<List<?>, Variant> func = LIST_TYPE_MAPPER.get(clazz);
        if (func == null) {
            throw new IllegalStateException("No list mapping for class " + clazz.getSimpleName());
        }

        return func.apply(list);
    }

    /**
     * Convert list of maps to {@link Container} array.
     *
     * @param list of maps
     * @return container array
     * @throws InconsistentElementTypesListException if internally some map contains list of elements with different types
     */
    private static Container[] toContainersArray(List<?> list) throws InconsistentElementTypesListException {
        Container[] array = new Container[list.size()];

        int i = 0;
        for (Object element : list) {
            array[i++] = toContainer((Map<String, Object>) element);
        }

        return array;
    }

    /**
     * Convert map to {@link Container}
     *
     * @param map the map
     * @return the container
     * @throws InconsistentElementTypesListException if internally map contains list of elements with different types
     */
    private static Container toContainer(Map<String, Object> map) throws InconsistentElementTypesListException {
        Container.ContainerBuilder builder = Container.builder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            builder.tag(TinyString.of(entry.getKey()), toVariant(value));
        }

        return builder.build();
    }
}
