package ru.kontur.vostok.hercules.elastic.adapter.index;

import ru.kontur.vostok.hercules.elastic.adapter.index.config.ConfigParser;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class IndexManager {
    private final Map<String, IndexMeta> meta;

    public IndexManager(Properties properties) {
        String path = PropertiesUtil.get(Props.CONFIG_PATH, properties).get().substring("file://".length());
        try (FileInputStream in = new FileInputStream(path)) {
            meta = ConfigParser.parse(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    //TODO: Map index name to log event properties (project, environment, etc...)
    //TODO: Use index pattern: `<project>-<environment>-<*>`
    //TODO: Also, can use predefined properties: `awesome-production-000001` -> {poject -> awesome, environment -> production}
    public IndexMeta meta(String index) {
        return meta.get(index);//TODO: replace with search by pattern
    }

    private static class Props {
        static Parameter<String> CONFIG_PATH =
                Parameter.stringParameter("config.path").
                        withDefault("file://indices.json").
                        withValidator(v -> {
                            final String prefix = "file://";
                            if (v.startsWith(prefix)) {
                                return ValidationResult.ok();
                            }
                            return ValidationResult.error("Value should start with " + prefix);
                        }).
                        build();
    }
}
