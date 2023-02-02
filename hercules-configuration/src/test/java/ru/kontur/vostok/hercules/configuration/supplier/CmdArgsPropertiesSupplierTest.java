package ru.kontur.vostok.hercules.configuration.supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CmdArgsPropertiesSupplier} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@DisplayName("CmdArgsPropertiesSupplier unit tests")
class CmdArgsPropertiesSupplierTest {

    @Test
    void test() {
        var supplier = new CmdArgsPropertiesSupplier(Map.of("key", "value"));

        Properties result = supplier.get();

        assertEquals(1, result.size());
        Entry<Object, Object> entry = CollectionUtils.getOnlyElement(result.entrySet());
        assertEquals("key", entry.getKey());
        assertEquals("value", entry.getValue());
    }
}