package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SentryStackFrame {

    @Unsupported
    private List<String> preContext;
    @Unsupported
    private List<String> postContext;
    @Unsupported
    private Map<String, String> vars;
    @Unsupported
    private List<Integer> framesOmitted;
    private String filename;
    private String function;
    @Unsupported
    private String module;
    private Integer lineno;
    private Integer colno;
    @Unsupported
    private String absPath;
    @Unsupported
    private String contextLine;
    @Unsupported
    private Boolean inApp;
@JsonProperty("package")
    private String _package;
    @Unsupported
@JsonProperty("native")
    private Boolean _native;
    @Unsupported
    private String platform;
    @Unsupported
    private String imageAddr;
    @Unsupported
    private String symbolAddr;
    @Unsupported
    private String instructionAddr;
    @Unsupported
    private Map<String, Object> unknown;
    @Unsupported
    private String rawFunction;

    public @Nullable String getPackage() {
        return _package;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getFunction() {
        return this.function;
    }

    public Integer getLineno() {
        return this.lineno;
    }

    public Integer getColno() {
        return this.colno;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public void setLineno(Integer lineno) {
        this.lineno = lineno;
    }

    public void setColno(Integer colno) {
        this.colno = colno;
    }

    public void setPackage(String _package) {
        this._package = _package;
    }
}
