package ru.kontur.vostok.hercules.configuration;

import ru.kontur.vostok.hercules.configuration.file.FileSource;
import ru.kontur.vostok.hercules.configuration.resource.ResourceSource;
import ru.kontur.vostok.hercules.configuration.zk.ZkSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public final class Sources {
    private static final Map<String, Source> SOURCES;

    static {
        Map<String, Source> sources = new HashMap<>();

        sources.put("file", new FileSource());
        sources.put("zk", new ZkSource());
        sources.put("resource", new ResourceSource());

        SOURCES = sources;
    }

    public static InputStream load(String sourcePath) {
        String schema = getSchemaFromSource(sourcePath);
        Source source = SOURCES.get(schema);
        if (source == null) {
            throw new IllegalArgumentException("Unknown schema " + schema);
        }
        return source.load(sourcePath);
    }

    public static <R> R load(String sourcePath, ParserFunction<R> parser) {
        try (var reader = new BufferedReader(new InputStreamReader(load(sourcePath), StandardCharsets.UTF_8))) {
            return parser.parse(reader);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String getSchemaFromSource(String source) {
        int schemaLength = source.indexOf(':');
        if (schemaLength == -1) {
            throw new IllegalArgumentException("Invalid source string");
        }
        return source.substring(0, schemaLength).toLowerCase();
    }

    @FunctionalInterface
    public interface ParserFunction<R> {
        R parse(BufferedReader reader) throws IOException;
    }

    private Sources() {
        /* static class */
    }
}
