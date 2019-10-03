package ru.kontur.vostok.hercules.http.path;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class PathTemplate {
    private static PathTemplate ROOT = new PathTemplate(true, "/", new Part[0]);

    private final boolean exactPath;
    private final String pathTemplate;
    final Part[] allParts;

    /**
     * Path template is valid URL path with named placeholders or path parameters.
     * <p>
     * Path parameter is defined with started colon symbol and separated by slash.
     * <p>
     * Sample:<br>
     * Path template {@code "/path/:book/:page"} has two path parameters {@code book} and {@code page}.<br>
     * This path template accepts path {@code "/path/thehitchhikersguidetothegalaxy/42"},
     * but do not accept {@code "/path"} and {@code "/path/with/extra/levels"}/.
     *
     * @param pathTemplate path template
     * @return deserialized path template
     */
    public static PathTemplate of(String pathTemplate) {
        if (pathTemplate.equals("/")) {
            return ROOT;
        }

        String[] split = PathUtil.normalizePath(pathTemplate);
        Part[] parts = new Part[split.length];
        boolean exactPath = true;
        //TODO: remove trailing slashes
        for (int i = 0; i < split.length; i++) {
            String part = split[i];
            if (part.startsWith(":")) {
                exactPath = false;
                parts[i] = new Part(false, part.substring(1));
            } else {
                parts[i] = new Part(true, part);
            }
        }
        return new PathTemplate(exactPath, pathTemplate, parts);
    }

    public boolean isExactPath() {
        return exactPath;
    }

    public int size() {
        return allParts.length;
    }

    public ExactPathMatcher toExactMatcher() {
        if (!exactPath) {
            throw new IllegalStateException("Path template should be exact path");
        }
        return new ExactPathMatcher();
    }

    public PathTemplateMatcher toMatcher() {
        return new PathTemplateMatcher();
    }

    private PathTemplate(boolean exactPath, String pathTemplate, Part[] allParts) {
        this.exactPath = exactPath;
        this.pathTemplate = pathTemplate;
        this.allParts = allParts;
    }

    public class ExactPathMatcher {
        public boolean match(String path) {
            return pathTemplate.equals(path);
        }

        public boolean match(Path path) {
            return match(path.getPath());
        }
    }

    public class PathTemplateMatcher {
        private final int size;

        PathTemplateMatcher() {
            size = PathTemplate.this.size();
        }

        public Map<String, String> match(Path path) {
            if (size != path.size()) {
                return Collections.emptyMap();
            }

            //TODO: It would be nice to DRY (don't repeat yourself) and process exact path and path template parts separately (without iterating same collection twice)
            String[] normalizedPath = path.getNormalizedPath();
            for (int i = 0; i < size; i++) {
                Part part = allParts[i];
                String normalizedPathPart = normalizedPath[i];
                if (part.exactPath && !part.pathOrName.equals(normalizedPathPart)) {
                        return Collections.emptyMap();
                }
            }

            Map<String,String> pathParameters = new HashMap<>();
            for (int i = 0; i < size; i++) {
                Part part = allParts[i];
                if (part.exactPath) {
                    continue;
                }
                pathParameters.put(part.pathOrName, normalizedPath[i]);
            }
            return pathParameters;
        }
    }

    private static class Part {
        private final boolean exactPath;
        private final String pathOrName;

        Part(boolean exactPath, String pathOrName) {
            this.exactPath = exactPath;
            this.pathOrName = pathOrName;
        }
    }
}
