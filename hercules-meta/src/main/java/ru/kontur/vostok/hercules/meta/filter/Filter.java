package ru.kontur.vostok.hercules.meta.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;

/**
 * @author Gregory Koshelev
 */
public class Filter {
    private String path;
    private Condition condition;

    /**
     * Backing field for h-path.
     */
    private transient HPath hPath;

    public Filter() {
    }

    public Filter(String path, Condition condition) {
        this.path = path;
        this.condition = condition;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;

        hPath = new HPath(path);
    }

    public Condition getCondition() {
        return condition;
    }
    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public boolean test(Container container) {
        Variant tagValue = hPath.extract(container);
        return condition.test(tagValue);
    }

    @JsonIgnore
    public HPath getHPath() {
        return hPath;
    }
}
