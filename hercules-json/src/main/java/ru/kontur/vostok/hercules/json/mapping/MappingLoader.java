package ru.kontur.vostok.hercules.json.mapping;

import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.json.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.protocol.hpath.HTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Load mapping configuration from file.
 * <p>
 * Supports two types of mappers:
 * <ul>
 *     <li>{@link TagMapper} maps an individual event tag using provided field name,
 *     <li>{@link MoveMapper} maps a whole container preserving tag names as fields.
 * </ul>
 * <p>
 * File format rules:
 * <ul>
 *   <li>Commentary is supported: it should start on new line with symbol {@code '#'},
 *   <li>tag mapping should be in form
 *       {@code "<sourcePath> to <destinationPath>[ using <transformerClass>]"},<br>
 *       {@link Transformer} is optional since {@link Transformer#PLAIN} is default one,
 *       paths should be a valid non-empty {@link HPath} (even for {@code destinationPath} in JSON-document),
 *   <li>move mapping should be in form
 *       {@code "<sourcePath>/* to <destinationPath>/*"},<br>
 *       paths can be empty, here.
 * </ul>
 * <p>
 * Sample:
 * <pre>
 * {@code
 * # Render structured exception as string stack trace using ExceptionToStacktraceTransformer:
 * exception to stackTrace using ru.kontur.vostok.hercules.elastic.sink.mapping.ExceptionToStackTraceTransformer
 *
 * # Move all tags from properties container to upper level:
 * properties/* to *
 * }
 * </pre>
 *
 * @author Gregory Koshelev
 */
public class MappingLoader {
    private static final Pattern PATH_REGEXP = Pattern.compile("(\\*)|(([a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*)(/\\*)?)");
    private static final Pattern CLASS_NAME_REGEXP = Pattern.compile("(([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*[a-zA-Z_$][a-zA-Z0-9_$]*)");

    /**
     * Loads mappers from file.
     *
     * @param mappingFile configuration file path supports schemas from {@link Sources}
     * @return mapper tree is based on {@link HPath}
     */
    public static HTree<Mapper> loadMapping(String mappingFile) {
        HTree<Mapper> mappers = new HTree<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(Sources.load(mappingFile), StandardCharsets.UTF_8))) {
            String line;
            for (int lineNumber = 1; (line = reader.readLine()) != null; lineNumber++) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] segments = line.split(" ");
                if ((segments.length != 3 && segments.length != 5)
                        || !segments[1].equals("to")
                        || (segments.length == 5 && !segments[3].equals("using"))) {
                    throw new IllegalArgumentException("Mapping file " + mappingFile + " has invalid line number " + lineNumber);
                }

                String source = segments[0];
                boolean isMove = source.endsWith("*");
                Matcher sourceMatcher = PATH_REGEXP.matcher(source);
                if (!sourceMatcher.matches()) {
                    throw new IllegalArgumentException("Invalid source path '" + source + "'");
                }
                String sourcePath = sourceMatcher.group(3);

                String destination = segments[2];
                Matcher destinationMatcher = PATH_REGEXP.matcher(destination);
                if (!destinationMatcher.matches() || (!isMove && destination.endsWith("*"))) {
                    throw new IllegalArgumentException("Invalid destination path '" + destination + "'");
                }
                String destinationPath = destinationMatcher.group(3);

                if (isMove) {
                    mappers.put(
                            sourcePath != null ? HPath.fromPath(sourcePath) : HPath.empty(),
                            new MoveMapper(destinationPath != null ? destinationPath : ""));
                } else {
                    boolean withTransformer = (segments.length == 5);
                    Transformer transformer = withTransformer
                            ? Transformer.fromClass(validateClassName(segments[4]))
                            : Transformer.PLAIN;

                    mappers.put(HPath.fromPath(source), new TagMapper(destination, transformer));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read mapping file " + mappingFile, ex);
        }
        return mappers;
    }

    private static String validateClassName(String className) {
        if (!CLASS_NAME_REGEXP.matcher(className).matches()) {
            throw new IllegalArgumentException("Invalid class name '" + className + "'");
        }
        return className;
    }
}
