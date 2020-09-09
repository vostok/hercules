package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.json.format.combiner.Combiner;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.protocol.hpath.HTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load mapping configuration from file.
 * <p>
 * Supported mappers:
 * <ul>
 *     <li>{@link TransformMapper} maps an individual event tag to a field with the provided name,
 *     <li>{@link MoveMapper} maps a whole container preserving tag names as fields.
 *     <li>{@link CombineMapper} maps multiple tags to a single field with the provided name.
 * </ul>
 * <p>
 * File format rules:
 * <ul>
 *   <li>Commentary is supported: it should start on new line with symbol {@code '#'},
 *   <li><b>transform</b> mapping should be in form
 *       {@code "transform <sourcePath> to <destinationPath>[ using <transformerClass>]"},<br>
 *       {@link Transformer} is optional since {@link Transformer#PLAIN} is default one,
 *       paths should be a valid non-empty {@link HPath} (even for {@code destinationPath} in JSON-document),
 *   <li><b>move</b> mapping should be in form
 *       {@code "move <sourcePath>/* to <destinationPath>/*[ except <exceptedTag1>,<exceptedTag2>,...]"},<br>
 *       source and destinations paths can be empty (should be {@code "*"} in this case),
 *       excepted tags are optional: list of tags which should not move to destination path,
 *   <li><b>combine</b> mapping should be in form
 *       {@code "combine <sourcePath1>,<sourcePath2>[,...] to <destinationPath> using <combinerClass>"}.
 * </ul>
 * <p>
 * Sample:
 * <pre>
 * {@code
 * # Render structured exception as string stack trace using ExceptionToStacktraceTransformer:
 * transform exception to stackTrace using ru.kontur.vostok.hercules.elastic.sink.format.ExceptionToStackTraceTransformer
 *
 * # Move all tags from properties container to upper level:
 * move properties/* to *
 * }
 * </pre>
 *
 * @author Gregory Koshelev
 */
public class MappingLoader {
    private static final Pattern PATH_REGEXP = Pattern.compile("[a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*");
    private static final Pattern PATH_WITH_STAR_REGEXP = Pattern.compile("(\\*)|(([a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*)/\\*)");

    /**
     * Loads mappers configuration from file.
     *
     * @param mappingFile configuration file path supports schemas from {@link Sources}
     * @return configured mapping
     */
    public static Mapping loadMapping(String mappingFile) {
        List<Mapper> mappers = new ArrayList<>();
        HTree<Boolean> mappableTags = new HTree<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(Sources.load(mappingFile), StandardCharsets.UTF_8))) {
            String line;
            for (int lineNumber = 1; (line = reader.readLine()) != null; lineNumber++) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] segments = line.split(" ");
                switch (segments[0]) {
                    case "transform":
                        loadTransformMapper(segments, lineNumber, mappers, mappableTags);
                        break;
                    case "move":
                        loadMoveMapper(segments, lineNumber, mappers, mappableTags);
                        break;
                    case "combine":
                        loadCombineMapper(segments, lineNumber, mappers, mappableTags);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mapper " + segments[0]);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read mapping file " + mappingFile, ex);
        }
        return new Mapping(mappers, mappableTags);
    }

    private static void loadTransformMapper(String[] segments, int lineNumber, List<Mapper> mappers, HTree<Boolean> mappableTags) {
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

        HPath sourcePath = HPath.fromPath(source);

        mappers.add(new TransformMapper(sourcePath, destination, transformer));
        mappableTags.put(sourcePath, Boolean.TRUE);
    }

    private static void loadMoveMapper(String[] segments, int lineNumber, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        boolean withExceptedTags = (segments.length == 6);

        if ((segments.length != 4 && !withExceptedTags)  || !"to".equals(segments[2]) || !segments[1].endsWith("*") || !segments[3].endsWith("*")) {
            throw new IllegalArgumentException("Unexpected form of the move mapper, line: " + lineNumber);
        }
        if (withExceptedTags && !"except".equals(segments[4])) {
            throw new IllegalArgumentException("Unknown token '" + segments[4] + "', line: " + lineNumber);
        }

        String source = segments[1];
        String destination = segments[3];
        String exceptedTagsRaw = withExceptedTags ? segments[5] : "";

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

        Set<TinyString> exceptedTags = withExceptedTags
                ? Stream.of(exceptedTagsRaw.split(",")).map(TinyString::of).collect(Collectors.toSet())
                : Collections.emptySet();

        HPath sourcePath = filteredSource != null ? HPath.fromPath(filteredSource) : HPath.empty();

        mappers.add(new MoveMapper(sourcePath, filteredDestination != null ? filteredDestination : "", exceptedTags));
        mappableTags.put(sourcePath, Boolean.TRUE);
    }

    private static void loadCombineMapper(String[] segments, int lineNumber, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        if (segments.length != 6 || !"to".equals(segments[2]) || !"using".equals(segments[4])) {
            throw new IllegalArgumentException("Unexpected form of the combine mapper, line: " + lineNumber);
        }

        String sourcesRaw = segments[1];
        String destination = segments[3];
        String combinerClass = segments[5];

        String[] sources = sourcesRaw.split(",");
        for (String source : sources) {
            Matcher sourceMatcher = PATH_REGEXP.matcher(source);
            if (!sourceMatcher.matches()) {
                throw new IllegalArgumentException("Invalid source path '" + source + "', line: " + lineNumber);
            }
        }

        Matcher destinationMatcher = PATH_REGEXP.matcher(destination);
        if (!destinationMatcher.matches()) {
            throw new IllegalArgumentException("Invalid destination path '" + destination + "'");
        }

        Combiner combiner = Combiner.fromClass(combinerClass);

        List<HPath> sourcePaths = Stream.of(sources).map(HPath::fromPath).collect(Collectors.toList());

        mappers.add(new CombineMapper(sourcePaths, destination, combiner));
        for (HPath sourcePath : sourcePaths) {
            mappableTags.put(sourcePath, Boolean.TRUE);
        }
    }
}
