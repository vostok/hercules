package ru.kontur.vostok.hercules.elastic.adapter.event;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.collection.ArrayUtil;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Create Hercules event
 * @author Gregory Koshelev
 */
public class LogEventMapper {
    private static final TinyString TIMESTAMP_FIELD = TinyString.of("@timestamp");//TODO: should be configurable

    private static final Map<Class<?>, Function<Object, Variant>> TYPE_MAPPER;
    private static final Map<Class<?>, Function<List<?>, Variant>> LIST_TYPE_MAPPER;

    private static final Set<TinyString> LOG_EVENT_TAGS =
            new HashSet<>(Arrays.asList(
                    LogEventTags.LEVEL_TAG.getName(),
                    LogEventTags.MESSAGE_TAG.getName(),
                    LogEventTags.MESSAGE_TEMPLATE_TAG.getName(),
                    LogEventTags.EXCEPTION_TAG.getName(),
                    LogEventTags.STACK_TRACE_TAG.getName()));

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

    public static Result<Event, String> from(Map<String, Object> document, Map<TinyString, Variant> properties, String index) {
        Map<TinyString, Variant> tags;
        try {
            tags = processMap(document);
        } catch (InconsistentElementTypesListException e) {
            return Result.error("Document contains list elements of different types");
        }

        Variant variant = tags.get(TIMESTAMP_FIELD);
        if (variant == null) {
            return Result.error("Document does not contain timestamp field");
        }
        if (variant.getType() != Type.STRING) {
            return Result.error("Provided timestamp should be string but got " + variant.getType());
        }

        final String dateTime = new String((byte[]) variant.getValue());

        final ZonedDateTime zonedDateTime;
        try {
            zonedDateTime = (ZonedDateTime) DateTimeFormatter.ISO_DATE_TIME.parseBest(
                    dateTime,
                    ZonedDateTime::from,
                    temporal -> LocalDateTime.from(temporal).atZone(ZoneId.of("UTC")));
        } catch (DateTimeParseException ex) {
            return Result.error("Provided timestamp is invalid: " + dateTime);
        }

        if (zonedDateTime.isBefore(ZonedDateTime.now().minusYears(1)) || zonedDateTime.isAfter(ZonedDateTime.now().plusMonths(1))) {
            return Result.error("Provided timestamp is outside the processable range [now - year; now + month]: " + dateTime);
        }

        long timestamp = TimeUtil.dateTimeToUnixTicks(zonedDateTime);
        UUID eventId = UuidGenerator.getClientInstance().next();

        EventBuilder eventBuilder = EventBuilder.create(timestamp, eventId).version(1);
        for (TinyString k : LOG_EVENT_TAGS) {
            Variant v = tags.remove(k);
            if (v != null) {
                eventBuilder.tag(k, v);
            }
        }

        Container.ContainerBuilder propertiesContainer = Container.builder();
        propertiesContainer.tags(tags);
        propertiesContainer.tags(properties);
        propertiesContainer.tag(ElasticSearchTags.ELK_INDEX_TAG.getName(), Variant.ofString(index));//TODO: Move to IndexMeta / IndexManager

        eventBuilder.tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(propertiesContainer.build()));
        return Result.ok(eventBuilder.build());
    }

    /**
     * Convert map to Variant format
     *
     * @param map Map from json
     * @return map in variant format of data
     */
    private static Map<TinyString, Variant> processMap(Map<String, Object> map) throws InconsistentElementTypesListException {
        Map<TinyString, Variant> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            result.put(TinyString.of(entry.getKey()), toVariant(value));
        }

        return result;
    }

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

    private static Container[] toContainersArray(List<?> list) throws InconsistentElementTypesListException {
        Container[] array = new Container[list.size()];

        int i = 0;
        for (Object element : list) {
            array[i++] = toContainer((Map<String, Object>) element);
        }

        return array;
    }

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