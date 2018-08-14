package ru.kontur.vostok.hercules.stream.sink;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamSinkDaemon {
    private static final Object lock = new Object();

    private static CuratorClient curatorClient;
    private static StreamSink streamSink;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

        //TODO: Validate sinkProperties
        if (!sinkProperties.containsKey("derived")) {
            System.out.println("Validation fails (sink.properties): 'derived' should be specified");
            return;
        }

        try {
            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);

            String derivedName = sinkProperties.getProperty("derived");
            Optional<Stream> derivedOptional = streamRepository.read(derivedName);
            if (!derivedOptional.isPresent()) {
                throw new IllegalArgumentException("Unknown derived stream");
            }
            Stream derived = derivedOptional.get();
            if (!(derived instanceof DerivedStream)) {
                throw new IllegalArgumentException("Specified stream isn't derived one");
            }

            streamSink = new StreamSink(streamsProperties, (DerivedStream) derived);
            streamSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(StreamSinkDaemon::shutdown));

        System.out.println("Stream Sink Daemon started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Prepare Stream Sink Daemon to be shutdown");

        try {
            if (streamSink != null) {
                streamSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Stream Sink Daemon shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
