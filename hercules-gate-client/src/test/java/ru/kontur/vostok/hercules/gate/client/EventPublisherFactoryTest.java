package ru.kontur.vostok.hercules.gate.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Petr Demenev
 */
class EventPublisherFactoryTest {

    @Test
    void getProjectTest() {
        assertEquals("_project", EventPublisherFactory.getProject());
    }

    @Test
    void getSubprojectTest() {
        assertFalse(EventPublisherFactory.getSubproject().isPresent());
    }

    @Test
    void getEnvironmentTest() {
        assertEquals("_env", EventPublisherFactory.getEnvironment());
    }
}
