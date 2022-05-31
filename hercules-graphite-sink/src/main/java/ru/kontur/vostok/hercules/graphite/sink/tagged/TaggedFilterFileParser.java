package ru.kontur.vostok.hercules.graphite.sink.tagged;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser of tagged filter list file.
 *
 * @author Aleksandr Yuferov
 */
public class TaggedFilterFileParser {
    private static final Pattern BLANK_OR_COMMENT_LINE_PATTERN = Pattern.compile("^\\s*$|^\\s*#.*");

    /**
     * Parse data.
     *
     * @param reader Reader of character sequence to parse.
     * @return Result of the parsing.
     * @throws IOException Can be thrown by {@link BufferedReader#readLine()}
     */
    public ParseResult parse(BufferedReader reader) throws IOException {
        Context context = new Context(reader);
        while (context.hasNextNotEmptyLine()) {
            parseRule(context);
        }
        return new ParseResult(context);
    }

    private void parseRule(Context context) {
        for (String conditionString : context.line.split(";")) {
            Tag tag = extractTag(context, conditionString.strip());
            context.addTagCondition(tag);
        }
    }

    private Tag extractTag(Context context, String condition) {
        int eqIndex = condition.indexOf("=");
        if (eqIndex == -1) {
            throw new IllegalArgumentException(String.format(
                    "an error occurred while parsing file line %d: missing '=' char in condition '%s'",
                    context.lineNumber, condition
            ));
        }
        String key = condition.substring(0, eqIndex);
        String value = condition.substring(eqIndex + 1);
        return new Tag(key, value);
    }

    private static class Context {
        final BufferedReader reader;
        final Map<Tag, List<Integer>> tagsMap = new HashMap<>();
        final List<Integer> ruleConditionCount = new ArrayList<>();
        int lineNumber = 0;
        int ruleIndex = -1;
        String line;

        Context(BufferedReader reader) {
            this.reader = reader;
        }

        boolean hasNextNotEmptyLine() throws IOException {
            do {
                line = reader.readLine();
                lineNumber++;
            } while (line != null && BLANK_OR_COMMENT_LINE_PATTERN.matcher(line).matches());
            if (line != null) {
                ruleIndex++;
                ruleConditionCount.add(0);
                return true;
            }
            return false;
        }

        void addTagCondition(Tag tag) {
            tagsMap.computeIfAbsent(tag, (k) -> new ArrayList<>()).add(ruleIndex);
            ruleConditionCount.set(ruleIndex, ruleConditionCount.get(ruleIndex) + 1);
        }

        List<RuleCondition> createRuleConditions() {
            return tagsMap.entrySet().stream()
                    .map(tagEntry -> {
                        Tag tag = tagEntry.getKey();
                        int[] rules = tagEntry.getValue().stream()
                                .mapToInt(Integer::intValue)
                                .toArray();
                        return RuleCondition.of(tag, rules);
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Result of the parsing.
     */
    public static class ParseResult {
        private final List<RuleCondition> conditions;
        private final int[] ruleConditionCount;

        ParseResult(Context context) {
            this.conditions = context.createRuleConditions();
            this.ruleConditionCount = context.ruleConditionCount.stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
        }

        /**
         * Count of listed in file rules.
         *
         * @return Count of listed in file rules.
         */
        public int getRulesCount() {
            return ruleConditionCount.length;
        }

        /**
         * Count of conditions of each rule by rule indices.
         *
         * @return Count of conditions of each rule by rule indices.
         */
        public int[] getRuleConditionCount() {
            return ruleConditionCount;
        }

        /**
         * List of unique rules conditions.
         *
         * @return List of unique rules conditions.
         */
        public List<RuleCondition> getConditions() {
            return conditions;
        }
    }
}
