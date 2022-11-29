package ru.kontur.vostok.hercules.sentry.client.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.client.SentryConverterUtil;
import ru.kontur.vostok.hercules.sentry.client.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.App;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Browser;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.ContextContainer;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Device;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Gpu;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Message;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.OperatingSystem;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SdkVersion;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryEvent;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryException;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryLevel;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryRuntime;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackFrame;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.User;
import ru.kontur.vostok.hercules.sentry.client.impl.converters.ContextConverter;
import ru.kontur.vostok.hercules.sentry.client.impl.converters.SentryExceptionConverter;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Convert Hercules event to Sentry event
 *
 * @author Tatyana Tokmyanina
 */
public class SentryEventConverterImpl implements SentryEventConverter {
    private static final Set<String> PLATFORMS = Set.of(
            "as3",
            "c",
            "cfml",
            "cocoa",
            "csharp",
            "go",
            "groovy",
            "java",
            "javascript",
            "native",
            "node",
            "objc",
            "other",
            "perl",
            "php",
            "python",
            "ruby"
    );

    private static final Set<String> STANDARD_CONTEXTS = Set.of(
            "user",
            "device",
            "os",
            "runtime",
            "browser",
            "app",
            "gpu"
    );

    private static final Map<String, BiConsumer<SentryEvent, Object>> STANDARD_PROPERTIES_CONVERTERS
            = Collections.unmodifiableMap(fillFunctionsMap());

    private final SdkVersion sdkVersion;
    private final Parser<SentryLevel> sentryLevelParser;
    private final ContextConverter<User> userConverter;
    private final ContextConverter<Device> deviceConverter;
    private final ContextConverter<Gpu> gpuConverter;
    private final ContextConverter<OperatingSystem> osConverter;
    private final ContextConverter<App> appConverter;
    private final ContextConverter<Browser> browserConverter;
    private final ContextConverter<SentryRuntime> runtimeConverter;

    public SentryEventConverterImpl(String herculesVersion) {
        this.sdkVersion = new SdkVersion()
                .setName("hercules-sentry-sink")
                .setVersion(herculesVersion);
        this.userConverter = new ContextConverter<>(User.class);
        this.deviceConverter = new ContextConverter<>(Device.class);
        this.gpuConverter = new ContextConverter<>(Gpu.class);
        this.osConverter = new ContextConverter<>(OperatingSystem.class);
        this.appConverter = new ContextConverter<>(App.class);
        this.browserConverter = new ContextConverter<>(Browser.class);
        this.runtimeConverter = new ContextConverter<>(SentryRuntime.class);

        sentryLevelParser = new Parser<>() {
            private final SentryLevelParserImpl innerParser = new SentryLevelParserImpl();

            @Override
            public @NotNull
            ParsingResult<SentryLevel> parse(@Nullable String value) {
                var result = innerParser.parse(value);
                if (result.hasValue()) {
                    return ParsingResult.of(SentryLevel.valueOf(result.get().name()));
                }
                return ParsingResult.empty();
            }
        };
    }

    @Override
    public SentryEventImpl convert(Event logEvent) {

        SentryEvent event = new SentryEvent();
        event.setTimestamp(TimeUtil.unixTicksToInstant(logEvent.getTimestamp()));

        event.setSdk(sdkVersion);
        event.setEventId(logEvent.getUuid());

        Container payload = logEvent.getPayload();
        setDataFromPayload(event, payload);

        event.setFingerprint(new ArrayList<>());
        event.setTags(new HashMap<>());
        event.setExtra(new HashMap<>());

        Map<String, Map<String, Variant>> contexts = ContainerUtil
                .extract(payload, CommonTags.PROPERTIES_TAG)
                .map(container -> setDataFromProperties(event, container))
                .orElse(Map.of());

        ContainerUtil.extract(payload, LogEventTags.MESSAGE_TEMPLATE_TAG).ifPresent(value -> {
            if (event.getFingerprint() != null && event.getFingerprint().isEmpty()
                    && event.getExceptions() == null) {
                event.getFingerprint().add(value);
            }
        });

        setDataFromContexts(event, contexts);

        return new SentryEventImpl(event);
    }

    private void setDataFromPayload(SentryEvent event, Container payload) {
        ContainerUtil.extract(payload, LogEventTags.LEVEL_TAG)
                .map(levelString -> sentryLevelParser.parse(levelString).orElse(null))
                .ifPresent(event::setLevel);

        ContainerUtil.extract(payload, LogEventTags.MESSAGE_TAG).ifPresent(value -> event.setMessage(new Message().setMessage(value)));

        ContainerUtil.extract(payload, LogEventTags.EXCEPTION_TAG).ifPresent(value -> {
            List<SentryException> exceptions = SentryExceptionConverter.convertException(value);
            event.setExceptions(exceptions);
            String platform = extractPlatform(exceptions);
            if (platform != null) {
                event.setPlatform(platform);
            }
        });

        ContainerUtil.extract(payload, LogEventTags.STACK_TRACE_TAG)
                .ifPresent(value -> {
                    Map<String, String> tags = new HashMap<>();
                    tags.put("stackTrace", value);
                    event.setTags(tags);
                });
    }

    private Map<String, Map<String, Variant>> setDataFromProperties(SentryEvent event,
            Container properties) {
        Map<String, Map<String, Variant>> contexts = new HashMap<>();
        for (Map.Entry<TinyString, Variant> entry : properties.tags().entrySet()) {
            String tagKey = entry.getKey().toString();
            Variant tagValue = entry.getValue();

            var stdConverter = STANDARD_PROPERTIES_CONVERTERS.get(tagKey);
            if (stdConverter != null) {
                stdConverter.accept(event, SentryConverterUtil.extractObject(tagValue));
                continue;
            }

            String extraField = SentryConverterUtil.cutOffPrefixIfExists("extra", tagKey)
                    .orElse(null);
            if (extraField != null) {
                event.getExtra().put(extraField, SentryConverterUtil.extractObject(tagValue));
                continue;
            }
            if (trySetContextValue(contexts, tagKey, tagValue)) {
                continue;
            }

            if (tagValue.getType().isPrimitive()) {
                event.getTags().put(tagKey, SentryConverterUtil.sanitizeTagValue(tagValue));
            } else {
                event.getExtra().put(tagKey, SentryConverterUtil.extractObject(tagValue));
            }
        }
        return contexts;
    }

    private void setDataFromContexts(SentryEvent event,
            Map<String, Map<String, Variant>> rawContexts) {
        if (rawContexts == null) {
            return;
        }
        for (Entry<String, Map<String, Variant>> entry : rawContexts.entrySet()) {
            ContextContainer eventContexts = event.getContexts();
            switch (entry.getKey()) {
                case "user":
                    event.setUser(userConverter.convert(entry.getValue()));
                    continue;
                case "device":
                    eventContexts.setDevice(deviceConverter.convert(entry.getValue()));
                    continue;
                case "os":
                    eventContexts.setOs(osConverter.convert(entry.getValue()));
                    continue;
                case "runtime":
                    eventContexts.setRuntime(runtimeConverter.convert(entry.getValue()));
                    continue;
                case "browser":
                    eventContexts.setBrowser(browserConverter.convert(entry.getValue()));
                    continue;
                case "app":
                    eventContexts.setApp(appConverter.convert(entry.getValue()));
                    continue;
                case "gpu":
                    eventContexts.setGpu(gpuConverter.convert(entry.getValue()));
                    break;
            }
        }
    }

    private static String extractPlatform(final List<SentryException> exceptions) {
            return exceptions
                    .stream()
                    .flatMap(e -> {
                        assert e.getStacktrace() != null;
                        assert e.getStacktrace().getFrames() != null;
                        return e.getStacktrace().getFrames().stream();
                    })
                    .map(SentryStackFrame::getFilename)
                    .map(SentryConverterUtil::resolvePlatformByFileName)
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElse(null);
    }

    private static Map<String, BiConsumer<SentryEvent, Object>> fillFunctionsMap() {
        Map<String, BiConsumer<SentryEvent, Object>> functions = new HashMap<>();
        functions.put(SentryTags.PLATFORM_TAG.getNameAsString(), (event, value) -> {
            String platform = ((String) value).toLowerCase(Locale.ROOT);
            if (PLATFORMS.contains(platform)) {
                event.setPlatform(platform);
            }
        });
        functions.put(SentryTags.LOGGER_TAG.getNameAsString(),
                (event, value) -> event.setLogger((String) value));
        functions.put(SentryTags.TRACE_ID_TAG.getNameAsString(), (event, value) -> {
            if (event.getTransaction() == null) {
                event.setTransaction(value.toString());
            }
            event.getTags().put("traceId", value.toString());
        });
        functions.put(SentryTags.TRANSACTION_TAG.getNameAsString(),
                (event, value) -> event.setTransaction((String) value));
        functions.put(SentryTags.SERVER_NAME_TAG.getNameAsString(),
                (event, value) -> event.setServerName((String) value));
        functions.put(SentryTags.RELEASE_TAG.getNameAsString(),
                (event, value) -> event.setRelease((String) value));
        functions.put(CommonTags.ENVIRONMENT_TAG.getNameAsString(),
                (event, value) -> event.setEnvironment((String) value));
        functions.put(SentryTags.FINGERPRINT_TAG.getNameAsString(), (event, values) -> {
            if (event.getFingerprint() != null) {
                if (values instanceof String) {
                    event.getFingerprint().add((String)values);
                }
                else if (values instanceof Collection) {
                    Collection<?> val = (Collection<?>) values;
                    for (Object v: val) {
                        event.getFingerprint().add((String) v);
                    }
                }
                else {
                    event.getFingerprint().addAll(Arrays.asList((String[]) values));
                }
            }
        });
        return functions;
    }

    private boolean trySetContextValue(Map<String, Map<String, Variant>> contexts,
            String tagKeyString, Variant tagValue) {
        int dotIndex = tagKeyString.indexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String contextName = tagKeyString.substring(0, dotIndex);
        String innerName = tagKeyString.substring(dotIndex + 1);
        if (!STANDARD_CONTEXTS.contains(contextName)) {
            return false;
        }
        contexts.computeIfAbsent(contextName, (key) -> new HashMap<>())
                .put(innerName, tagValue);
        return true;
    }
}
