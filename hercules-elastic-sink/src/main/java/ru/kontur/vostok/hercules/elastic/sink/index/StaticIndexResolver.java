package ru.kontur.vostok.hercules.elastic.sink.index;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.util.Optional;
import java.util.Properties;

/**
 * Resolves the index name from statically specified name.
 *
 * @author Petr Demenev
 */
public class StaticIndexResolver extends IndexResolver {

    private final String indexName;

    public StaticIndexResolver(Properties properties) {
        super(properties);
        this.indexName = PropertiesUtil.get(Props.INDEX_NAME, properties).get();
    }

    @Override
    public Optional<String> resolve(Event event) {
        return Optional.of(indexName);
    }

    static class Props {
        static final Parameter<String> INDEX_NAME =
                Parameter.stringParameter("index.name").
                        required().
                        withValidator((v) -> {
                            if (!IndexValidator.isValidIndexName(v)) {
                                return ValidationResult.error("Invalid index name: " + v);
                            }
                            if (!IndexValidator.isValidLength(v)) {
                                return ValidationResult.error("Index name exceeds length limit");
                            }
                            return ValidationResult.ok();
                        }).
                        build();
    }
}
