package ru.kontur.vostok.hercules.sentry.client.impl;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryLevel;

/**
 * @author Tatyana Tokmyanina
 */
public class SentryLevelParserImplTest {

    SentryLevelParserImpl parser = new SentryLevelParserImpl();

    @Test
    public void shouldReturnSentryLevelIfValueIsCorrect() {
        Assert.assertEquals(SentryLevel.FATAL, parser.parse("fatal").get());
        Assert.assertEquals(SentryLevel.ERROR, parser.parse("error").get());
        Assert.assertEquals(SentryLevel.WARNING, parser.parse("warn").get());
        Assert.assertEquals(SentryLevel.WARNING, parser.parse("warning").get());
        Assert.assertEquals(SentryLevel.INFO, parser.parse("info").get());
        Assert.assertEquals(SentryLevel.DEBUG, parser.parse("debug").get());
        Assert.assertEquals(SentryLevel.FATAL, parser.parse("Fatal").get());
        Assert.assertEquals(SentryLevel.ERROR, parser.parse("Error").get());
        Assert.assertEquals(SentryLevel.WARNING, parser.parse("Warn").get());
        Assert.assertEquals(SentryLevel.WARNING, parser.parse("Warning").get());
        Assert.assertEquals(SentryLevel.INFO, parser.parse("Info").get());
        Assert.assertEquals(SentryLevel.DEBUG, parser.parse("Debug").get());
    }

    @Test
    public void shouldReturnErrorIfValueIsIncorrect() {
        Assert.assertTrue(parser.parse("incorrect").hasError());
    }

    @Test
    public void shouldReturnEmptyResultIfValueIsIncorrect() {
        Assert.assertTrue(parser.parse("").isEmpty());
        Assert.assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    public void prepareLevelTest() {
    }
}