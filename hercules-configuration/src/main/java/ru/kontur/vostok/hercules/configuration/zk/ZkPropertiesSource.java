package ru.kontur.vostok.hercules.configuration.zk;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.PropertiesSource;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.ReadResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ZkPropertiesSource implements PropertiesSource {
    /**
     * Load properties from ZooKeeper.
     * <p>
     * {@code source} has following format: {@code zk://host1:port1[,host2:port2,...]/path/to/properties}.<br>
     *
     * @param source properties source
     * @return properties
     */
    @Override
    public Properties load(@NotNull String source) {
        if (!source.startsWith("zk://")) {
            throw new IllegalArgumentException("Source should started with 'zk://'");
        }
        int nodesStartIndex = "zk://".length();
        int pathStartIndex = source.indexOf('/', nodesStartIndex);
        if (pathStartIndex == -1 || pathStartIndex == nodesStartIndex) {
            throw new IllegalArgumentException("Source should contain zk nodes and path to properties");
        }
        String nodes = source.substring(nodesStartIndex, pathStartIndex);
        String path = source.substring(pathStartIndex);

        Properties properties = new Properties();
        properties.setProperty("connectString", nodes);
        CuratorClient curatorClient = new CuratorClient(properties);
        curatorClient.start();
        try {
            ReadResult readResult;
            try {
                readResult = curatorClient.read(path);
            } catch (CuratorInternalException | CuratorUnknownException ex) {
                throw new IllegalStateException("Cannot read properties due to zk/curator exception", ex);
            }
            if (!readResult.isSuccess()) {
                throw new IllegalArgumentException("Properties not found");
            }
            Optional<byte[]> data = readResult.getData();
            try (InputStream in = new ByteArrayInputStream(data.get())) {
                return PropertiesReader.read(in);
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot read properties", ex);
            }
        } finally {
            curatorClient.stop();
        }
    }
}
