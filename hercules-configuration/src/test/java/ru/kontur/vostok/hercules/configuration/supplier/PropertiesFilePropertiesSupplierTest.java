package ru.kontur.vostok.hercules.configuration.supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;

/**
 * {@link PropertiesFilePropertiesSupplier} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PropertiesFilePropertiesSupplier unit tests")
class PropertiesFilePropertiesSupplierTest {

    @Test
    @DisplayName("should use default filename if no arguments given")
    void shouldUseDefaultFilenameIfNoArgumentsGiven() {
        Properties expected = new Properties();
        try (var mocked = Mockito.mockStatic(PropertiesLoader.class)) {
            mocked.when(() -> PropertiesLoader.load(any())).thenReturn(expected);

            var supplier = new PropertiesFilePropertiesSupplier(Map.of());

            Properties actual = supplier.get();

            mocked.verify(() -> PropertiesLoader.load(PropertiesSupplier.FILE_ARG_DEFAULT_VALUE));
            assertSame(expected, actual);
        }
    }

    @Test
    @DisplayName("should use file name from command line argument")
    void shouldUseFileNameFromCommandLineArgument() {
        Properties expected = new Properties();
        try (var mocked = Mockito.mockStatic(PropertiesLoader.class)) {
            mocked.when(() -> PropertiesLoader.load(any())).thenReturn(expected);

            var supplier = new PropertiesFilePropertiesSupplier(Map.of(
                    PropertiesSupplier.FILE_ARG_NAME, "file://some-other-application.properties"
            ));

            Properties actual = supplier.get();

            mocked.verify(() -> PropertiesLoader.load("file://some-other-application.properties"));
            assertSame(expected, actual);
        }
    }
}