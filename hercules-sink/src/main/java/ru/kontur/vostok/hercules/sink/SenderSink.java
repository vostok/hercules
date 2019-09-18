package ru.kontur.vostok.hercules.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class SenderSink extends Sink {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderSink.class);

    public SenderSink(
            ExecutorService executor,
            String applicationId,
            Properties properties,
            Sender sender,
            MetricsCollector metricsCollector) {
        super(
                executor,
                applicationId,
                properties,
                sender,
                Stream.of(PropertiesUtil.get(Props.PATTERN, properties).get()).
                        map(PatternMatcher::new).
                        collect(Collectors.toList()),
                EventDeserializer.parseAllTags(),
                metricsCollector);
    }

    private static class Props {
        static final Parameter<String[]> PATTERN =
                Parameter.stringArrayParameter("pattern").
                        required().
                        build();
    }
}
