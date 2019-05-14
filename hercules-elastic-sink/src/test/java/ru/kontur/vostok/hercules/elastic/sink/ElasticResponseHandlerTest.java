package ru.kontur.vostok.hercules.elastic.sink;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static ru.kontur.vostok.hercules.elastic.sink.ElasticResponseHandler.NON_RETRYABLE_ERRORS_CODES;
import static ru.kontur.vostok.hercules.elastic.sink.ElasticResponseHandler.RETRYABLE_ERRORS_CODES;

/**
 * @author tsypaev
 */
public class ElasticResponseHandlerTest {

    @Test
    public void shouldNotHaveEqualsErrors(){
        assertFalse(NON_RETRYABLE_ERRORS_CODES.stream().anyMatch(RETRYABLE_ERRORS_CODES::contains));
    }
}
