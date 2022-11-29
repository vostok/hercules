package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 *
 * Not used for sending events from Hercules to Sentry
 *
 * @author Tatyana Tokmyanina
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Unsupported {
}
