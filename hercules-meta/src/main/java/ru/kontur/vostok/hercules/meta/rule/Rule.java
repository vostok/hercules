package ru.kontur.vostok.hercules.meta.rule;

/**
 * @author Gregory Koshelev
 */
public class Rule {
    private String key;
    private String pattern;
    private String right;

    public Rule() {
    }

    public Rule(String key, String pattern, String right) {
        this.key = key;
        this.pattern = pattern;
        this.right = right;
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getRight() {
        return right;
    }
    public void setRight(String right) {
        this.right = right;
    }
}
