package ru.kontur.vostok.hercules.elastic.adapter.format.mapping;

import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads mapping configuration from file.
 * <p>
 * Supported mappers:
 * <ul>
 *   <li>{@link MoveMapper} maps a whole sub-document preserving field names as tags.</li>
 *   <li>{@link TransformMapper} maps an individual field to a tag with the provided name.</li>
 * </ul>
 * <p>
 * File format rules:
 * <ul>
 *   <li>Supports commentary line is started with symbol {@code '#'},</li>
 *   <li><b>move</b> mapping should be in form
 *       {@code "move <sourcePath>/* to <destinationPath>/*[ except <exceptedField1>,<exceptedField2>,...]"},<br>
 *       source and destination paths can be empty (should be {@code "*"} in this case),
 *       excepted fields are optional: list of fields which should not move to destination path,</li>
 *   <li><b>transform</b> mapping should be in form
 *       {@code "transform <sourcePath> to <destinationPath>[ using <transformerClass>]"},<br>
 *       {@link Transformer} is optional since {@link Transformer#PLAIN} is default one,
 *       paths should be a valid non-empty {@link HPath} (even for {@code sourcePath} in the JSON-document).</li>
 * </ul>
 * <p>
 * Sample:
 * <pre>
 * {@code
 * # Transform log-specific fields as is
 * transform level to level
 * transform message to message
 * transform messageTemplate to messageTemplate
 * transform exception to exception
 * transform stackTrace to stackTrace
 *
 * # Move all fields to properties except level, message, messageTemplate, exception and stackTrace
 * move * to properties/* except level,message,messageTemplate,exception,stackTrace
 * }
 * </pre>
 *
 * @author Gregory Koshelev
 */
public class MappingLoader {
    private static final Pattern PATH_REGEXP = Pattern.compile("[a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*");
    private static final Pattern PATH_WITH_STAR_REGEXP = Pattern.compile("(\\*)|(([a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*)/\\*)");

    /**
     * Load mappers configuration from file.
     *
     * @param mappingFile configuration file path
     * @return configured mapping
     */
    public static Mapping loadMapping(String mappingFile) {
        List<Mapper> mappers = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(Sources.load(mappingFile), StandardCharsets.UTF_8))) {
            String line;
            for (int lineNumber = 1; (line = reader.readLine()) != null; lineNumber++) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] segments = line.split(" ");
                switch (segments[0]) {
                    case "move":
                        loadMoveMapper(segments, lineNumber, mappers);
                        break;
                    case "transform":
                        loadTransformMapper(segments, lineNumber, mappers);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mapper " + segments[0]);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read mapping file " + mappingFile, ex);
        }
        return new Mapping(mappers);
    }

    private static void loadMoveMapper(String[] segments, int lineNumber, List<Mapper> mappers) {
        boolean withExceptedFields = (segments.length == 6);

        if ((segments.length != 4 && !withExceptedFields) || !"to".equals(segments[2]) || !segments[1].endsWith("*") || !segments[3].endsWith("*")) {
            throw new IllegalArgumentException("Unexpected form of the move mapper, line: " + lineNumber);
        }
        if (withExceptedFields && !"except".equals(segments[4])) {
            throw new IllegalArgumentException("Unknown token '" + segments[4] + "', line: " + lineNumber);
        }

        String source = segments[1];
        String destination = segments[3];
        String exceptedFieldsRow = withExceptedFields ? segments[5] : "";

        Matcher sourceMatcher = PATH_WITH_STAR_REGEXP.matcher(source);
        if (!sourceMatcher.matches()) {
            throw new IllegalArgumentException("Invalid source path '" + source + "', line: " + lineNumber);
        }
        String filteredSource = sourceMatcher.group(3);

        Matcher destinationMatcher = PATH_WITH_STAR_REGEXP.matcher(destination);
        if (!destinationMatcher.matches()) {
            throw new IllegalArgumentException("Invalid destination path '" + destination + "'");
        }
        String filteredDestination = destinationMatcher.group(3);

        Set<String> exceptedFields = withExceptedFields
                ? new HashSet<>(Arrays.asList(exceptedFieldsRow.split(",")))
                : Collections.emptySet();

        HPath destinationPath = filteredDestination != null ? HPath.fromPath(filteredDestination) : HPath.empty();

        mappers.add(new MoveMapper(filteredSource != null ? filteredSource : "", destinationPath, exceptedFields));
    }

    private static void loadTransformMapper(String[] segments, int lineNumber, List<Mapper> mappers) {
        boolean withTransformer = (segments.length == 6);

        if ((segments.length != 4 && !withTransformer) || !"to".equals(segments[2])) {
            throw new IllegalArgumentException("Unexpected form of the transform mapper, line: " + lineNumber);
        }
        if (withTransformer && !"using".equals(segments[4])) {
            throw new IllegalArgumentException("Unknown token '" + segments[4] + "', line: " + lineNumber);
        }

        String source = segments[1];
        String destination = segments[3];
        String transformerClass = withTransformer ? segments[5] : "";

        Matcher sourceMatcher = PATH_REGEXP.matcher(source);
        if (!sourceMatcher.matches()) {
            throw new IllegalArgumentException("Invalid source path '" + source + "', line: " + lineNumber);
        }

        Matcher destinationMatcher = PATH_REGEXP.matcher(destination);
        if (!destinationMatcher.matches()) {
            throw new IllegalArgumentException("Invalid destination path '" + destination + "'");
        }

        Transformer transformer = withTransformer
                ? Transformer.fromClass(transformerClass)
                : Transformer.PLAIN;

        HPath destinationPath = HPath.fromPath(source);

        mappers.add(new TransformMapper(source, destinationPath, transformer));
    }
}
