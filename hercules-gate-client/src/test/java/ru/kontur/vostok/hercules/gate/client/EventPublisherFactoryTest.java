package ru.kontur.vostok.hercules.gate.client;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Petr Demenev
 */
public class EventPublisherFactoryTest {

    @Test
    public void getProjectTest() {
        Assert.assertEquals("_project", EventPublisherFactory.getProject());
    }

    @Test
    public void getSubprojectTest() {
        Assert.assertFalse(EventPublisherFactory.getSubproject().isPresent());
    }

    @Test
    public void getEnvironmentTest() {
        Assert.assertEquals("_env", EventPublisherFactory.getEnvironment());
    }
}
