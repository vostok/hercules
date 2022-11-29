package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface JsonUnknown {
    @Nullable
    Map<String, Object> getUnknown();

    JsonUnknown setUnknown(@Nullable Map<String, Object> unknown);
}
