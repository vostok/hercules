package ru.kontur.vostok.hercules.configuration.supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doReturn;

/**
 * {@link CombinedPropertiesSupplier} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CombinedPropertiesSupplier unit tests")
class CombinedPropertiesSupplierTest {

    Map<String, String> cmdArgs;
    @Mock
    PropertiesSupplier cmdArgsPropertiesSupplier;
    @Mock
    PropertiesSupplier propertiesFilePropertiesSupplier;
    CombinedPropertiesSupplier supplier;

    @BeforeEach
    void prepare() {
        cmdArgs = new HashMap<>();
        supplier = new CombinedPropertiesSupplier(cmdArgs, cmdArgsPropertiesSupplier, propertiesFilePropertiesSupplier);
    }

    @Test
    @DisplayName("should throw exception if file extension is unsupported")
    void shouldThrowExceptionIfFileExtensionIsUnsupported() {
        cmdArgs.put(PropertiesSupplier.FILE_ARG_NAME, "file://some-file.txt");

        assertThrows(IllegalArgumentException.class, supplier::get);
    }

    @Test
    @DisplayName("should use default .properties-file if no cmd args found")
    void shouldUseDefaultPropertiesFileIfNoCmdArgsFound() {
        assumeTrue(cmdArgs.isEmpty());
        doReturn(PropertiesUtil.ofEntries(
                Map.entry("fromPropertiesFile", "value1")
        )).when(propertiesFilePropertiesSupplier).get();
        doReturn(PropertiesUtil.ofEntries(
                Map.entry("fromCommandLine", "value2")
        )).when(cmdArgsPropertiesSupplier).get();

        Properties properties = supplier.get();

        assertEquals(2, properties.size());
        assertEquals("value1", properties.getProperty("fromPropertiesFile"));
        assertEquals("value2", properties.getProperty("fromCommandLine"));
    }

    @Test
    @DisplayName("properties from cmd argument should have greater priority than from file")
    void propertiesFromCmdArgumentShouldHaveGreaterPriorityThanFromFile() {
        doReturn(PropertiesUtil.ofEntries(
                Map.entry("common", "value1")
        )).when(propertiesFilePropertiesSupplier).get();
        doReturn(PropertiesUtil.ofEntries(
                Map.entry("common", "value2")
        )).when(cmdArgsPropertiesSupplier).get();

        Properties properties = supplier.get();

        assertEquals(1, properties.size());
        assertEquals("value2", properties.getProperty("common"));
    }
}