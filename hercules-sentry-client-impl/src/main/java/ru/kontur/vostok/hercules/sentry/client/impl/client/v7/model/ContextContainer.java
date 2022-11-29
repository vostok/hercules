package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

/**
 * @author Aleksandr Yuferov
 */
public class ContextContainer {

    private Device device;
    private OperatingSystem os;
    private SentryRuntime runtime;
    private Browser browser;
    private App app;
    private Gpu gpu;

    public Device getDevice() {
        return this.device;
    }

    public OperatingSystem getOs() {
        return this.os;
    }

    public SentryRuntime getRuntime() {
        return this.runtime;
    }

    public Browser getBrowser() {
        return this.browser;
    }

    public App getApp() {
        return this.app;
    }

    public Gpu getGpu() {
        return this.gpu;
    }

    public ContextContainer setDevice(Device device) {
        this.device = device;
        return this;
    }

    public ContextContainer setOs(OperatingSystem os) {
        this.os = os;
        return this;
    }

    public ContextContainer setRuntime(SentryRuntime runtime) {
        this.runtime = runtime;
        return this;
    }

    public ContextContainer setBrowser(Browser browser) {
        this.browser = browser;
        return this;
    }

    public ContextContainer setApp(App app) {
        this.app = app;
        return this;
    }

    public ContextContainer setGpu(Gpu gpu) {
        this.gpu = gpu;
        return this;
    }
}
