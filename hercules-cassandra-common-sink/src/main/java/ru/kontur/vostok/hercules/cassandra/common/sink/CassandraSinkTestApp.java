package ru.kontur.vostok.hercules.cassandra.common.sink;

import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import javax.ws.rs.OPTIONS;
import java.util.Optional;

public class CassandraSinkTestApp extends AbstractCassandraSinkDaemon {

    public static void main(String[] args) {
        new CassandraSinkTestApp().run(args);
    }

    @Override
    protected String getDaemonName() {
        return "Just a working test app";
    }

    @Override
    protected String getDaemonId() {
        return "sink.cassandra.test-app";
    }

    @Override
    protected AbstractCassandraSender createCassandraSender(Session session) {
        return new AbstractCassandraSender(session) {
            @Override
            protected Optional<Object[]> convert(Event event) {

                final TagDescription<Optional<String>> keyTag = TagDescriptionBuilder.string("key")
                    .optional()
                    .build();
                final TagDescription<Optional<String>> valueTag = TagDescriptionBuilder.string("value")
                    .optional()
                    .build();

                final Optional<String> key = ContainerUtil.extract(event.getPayload(), keyTag);
                final Optional<String> value = ContainerUtil.extract(event.getPayload(), valueTag);

                if (key.isPresent() && value.isPresent()) {
                    return Optional.of(new Object[]{key.get(), value.get()});
                } else {
                    return Optional.empty();
                }
            }

            @Override
            protected String getPreparedStatement() {
                return "insert into test.kv (key, value) values (?, ?)";
            }
        };
    }
}
