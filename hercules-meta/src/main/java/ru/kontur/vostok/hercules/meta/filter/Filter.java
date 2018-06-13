package ru.kontur.vostok.hercules.meta.filter;

/**
 * @author Gregory Koshelev
 */
public class Filter {
    private String tag;
    private Condition condition;

    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }

    public Condition getCondition() {
        return condition;
    }
    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
