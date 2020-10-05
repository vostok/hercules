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
 *       source and destination paths can be empty (should be {@code "*"} in this case),
 *       excepted tags are optional: list of tags which should not move to the destination path,
 *   <li><b>combine</b> mapping should be in form
 *       {@code "combine <sourcePath1>,<sourcePath2>[,...] to <destinationPath> using <combinerClass>"},
 *   <li><b>project</b> mapping should be in form
 *       {@code "project <sourcePath> to <destinationPath> with <key>[ using <transformerClass>] and <value>[ using <transformerClass>]"},
 *       source and destination paths should be a valid {@link HPath},
 *       also, the destination path can be empty (refers to root {@code "/"}),
 *       key is the tag with field name,
 *       value is the tag with field value,
 *       {@link Transformer} is optional since {@link Transformer#PLAIN} is used by default.
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
    private static final Pattern PATH_REGEXP = Pattern.compile("/|([a-zA-Z0-9_.-]+(/[a-zA-Z0-9_.-]+)*)");
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

                try {
                    DslParser dsl = new DslParser(line);
                    switch (dsl.type()) {
                        case "transform":
                            loadTransformMapper(dsl, mappers, mappableTags);
                            break;
                        case "move":
                            loadMoveMapper(dsl, mappers, mappableTags);
                            break;
                        case "combine":
                            loadCombineMapper(dsl, mappers, mappableTags);
                            break;
                        case "project":
                            loadProjectMapper(dsl, mappers, mappableTags);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported mapper " + dsl.type());
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Invalid input at line " + lineNumber, ex);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Exception when read mapping file " + mappingFile, ex);
        }
        return new Mapping(mappers, mappableTags);
    }

    /**
     * {@code "transform <sourcePath> to <destinationPath>[ using <transformerClass>]"}
     */
    private static void loadTransformMapper(DslParser dsl, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        dsl.require("transform");

        String source = path(dsl);
        HPath sourcePath = HPath.fromPath(source);

        dsl.require("to");

        String destination = path(dsl);

        Transformer transformer = dsl.check("using")
                ? Transformer.fromClass(dsl.token())
                : Transformer.PLAIN;

        mappers.add(new TransformMapper(sourcePath, destination, transformer));
        mappableTags.put(sourcePath, Boolean.TRUE);
    }

    /**
     * {@code "move <sourcePath>/* to <destinationPath>/*[ except <exceptedTag1>,<exceptedTag2>,...]"}
     */
    private static void loadMoveMapper(DslParser dsl, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        dsl.require("move");

        String source = dsl.token();
        Matcher sourceMatcher = PATH_WITH_STAR_REGEXP.matcher(source);
        if (!sourceMatcher.matches()) {
            throw new IllegalArgumentException("Invalid source path '" + source + "'");
        }
        String filteredSource = sourceMatcher.group(3);
        HPath sourcePath = filteredSource != null ? HPath.fromPath(filteredSource) : HPath.empty();

        dsl.require("to");

        String destination = dsl.token();
        Matcher destinationMatcher = PATH_WITH_STAR_REGEXP.matcher(destination);
        if (!destinationMatcher.matches()) {
            throw new IllegalArgumentException("Invalid destination path '" + destination + "'");
        }
        String filteredDestination = destinationMatcher.group(3);

        Set<TinyString> exceptedTags = dsl.check("except")
                ? Stream.of(dsl.token().split(",")).map(TinyString::of).collect(Collectors.toSet())
                : Collections.emptySet();

        mappers.add(new MoveMapper(sourcePath, filteredDestination != null ? filteredDestination : "", exceptedTags));
        mappableTags.put(sourcePath, Boolean.TRUE);
    }

    /**
     * {@code "combine <sourcePath1>,<sourcePath2>[,...] to <destinationPath> using <combinerClass>"}
     */
    private static void loadCombineMapper(DslParser dsl, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        dsl.require("combine");

        String[] sources = paths(dsl);
        List<HPath> sourcePaths = Stream.of(sources).map(HPath::fromPath).collect(Collectors.toList());

        dsl.require("to");

        String destination = path(dsl);

        dsl.require("using");

        Combiner combiner = Combiner.fromClass(dsl.token());

        mappers.add(new CombineMapper(sourcePaths, destination, combiner));
        for (HPath sourcePath : sourcePaths) {
            mappableTags.put(sourcePath, Boolean.TRUE);
        }
    }

    /**
     * {@code "project <sourcePath> to <destinationPath> with <key>[ using <transformerClass>] and <value>[ using <transformerClass>]"}
     */
    private static void loadProjectMapper(DslParser dsl, List<Mapper> mappers, HTree<Boolean> mappableTags) {
        dsl.require("project");

        String source = path(dsl);
        HPath sourcePath = HPath.fromPath(source);

        dsl.require("to");

        String destination = path(dsl);

        dsl.require("with");

        String keyTag = dsl.token();

        Transformer keyTransformer = dsl.check("using")
                ? Transformer.fromClass(dsl.token())
                : Transformer.PLAIN;

        dsl.require("and");

        String valueTag = dsl.token();

        Transformer valueTransformer = dsl.check("using")
                ? Transformer.fromClass(dsl.token())
                : Transformer.PLAIN;

        mappers.add(
                new ProjectMapper(
                        sourcePath,
                        destination,
                        TinyString.of(keyTag), keyTransformer,
                        TinyString.of(valueTag), valueTransformer));
        mappableTags.put(sourcePath, Boolean.TRUE);
    }

    private static String path(DslParser reader) {
        String token = reader.token();
        requirePath(token);
        return token;
    }

    private static String[] paths(DslParser reader) {
        String token = reader.token();
        String[] paths = token.split(",");
        for (String path : paths) {
            requirePath(path);
        }
        return paths;
    }

    private static void requirePath(String path) {
        if (!PATH_REGEXP.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid path '" + path);
        }
    }

    /**
     * The mapping DSL parser
     */
    private static class DslParser {
        private final String[] tokens;
        private int position;

        DslParser(String mapping) {
            this.tokens = mapping.split(" ");
        }

        /**
         * Return the mapping type which is a first token.
         *
         * @return the mapping type
         */
        private String type() {
            return tokens[0];
        }

        /**
         * Return a token.
         * <p>
         * Sequential calls of this method return tokens one by one.
         *
         * @return a token
         * @throws IllegalArgumentException if there are no more tokens
         */
        private String token() {
            if (position >= tokens.length) {
                throw new IllegalArgumentException("Unexpected end of line");
            }
            return tokens[position++];
        }

        /**
         * Check if a token exists and equals to the specified one.
         * <p>
         * In case of success {@link #token()} will return a next token.
         *
         * @param token the expected token
         * @return {@code true} if the expected token is present, otherwise {@code false}
         */
        private boolean check(String token) {
            boolean result = position < tokens.length && token.equals(tokens[position]);
            if (result) {
                position++;
            }
            return result;
        }

        /**
         * Require {@link #token()} to return the specified token.
         *
         * @param token the expected token
         * @throws IllegalArgumentException if the expected token does not exist
         */
        private void require(String token) {
            if (!check(token)) {
                throw new IllegalArgumentException("Expects token '" + token + "'");
            }
        }
    }
}
