package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public abstract class AbstractBulkSinkDaemon {

    private CuratorClient curatorClient;
    private CommonBulkEventSink bulkEventSink;
    private BulkSender sender;

    public void run(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties streamProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

        curatorClient = new CuratorClient(curatorProperties);
        curatorClient.start();

        StreamRepository streamRepository = new StreamRepository(curatorClient);

        String streamName = streamProperties.getProperty("stream.name");
        Optional<Stream> stream = toUnchecked(() -> streamRepository.read(streamName));
        if (!stream.isPresent()) {
            throw new IllegalArgumentException("Unknown stream");
        }

        //TODO: Validate sinkProperties
        try {
            sender = createSender(sinkProperties);
            bulkEventSink = new CommonBulkEventSink(getDaemonName(), stream.get(), streamProperties, sender);
            bulkEventSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        System.out.println(String.format("%s sink daemon started for %d millis", getDaemonName(), System.currentTimeMillis() - start));
    }

    protected abstract BulkSender createSender(Properties sinkProperties);

    protected abstract String getDaemonName();

    private void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println(String.format("Prepare %s sink daemon to be shutdown", getDaemonName()));

        try {
            if (Objects.nonNull(bulkEventSink)) {
                bulkEventSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        try {
            if (Objects.nonNull(sender)) {
                sender.close();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        try {
            if (Objects.nonNull(curatorClient)) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        System.out.println(String.format("Finished %s sink daemon shutdown for %d millis", getDaemonName(), System.currentTimeMillis() - start));
    }
}
