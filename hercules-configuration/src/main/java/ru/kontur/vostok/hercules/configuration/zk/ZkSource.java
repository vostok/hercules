package ru.kontur.vostok.hercules.configuration.zk;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.configuration.Source;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.ReadResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ZkSource implements Source {
    /**
     * Load input stream from ZooKeeper.
     * <p>
     * {@code source} has following format: {@code zk://host1:port1[,host2:port2,...]/path/to/content}.<br>
     *
     * @param sourcePath source path
     * @return input stream
     */
    @Override
    public InputStream load(@NotNull String sourcePath) {
        if (!sourcePath.startsWith("zk://")) {
            throw new IllegalArgumentException("Source should started with 'zk://'");
        }
        int nodesStartIndex = "zk://".length();
        int pathStartIndex = sourcePath.indexOf('/', nodesStartIndex);
        if (pathStartIndex == -1 || pathStartIndex == nodesStartIndex) {
            throw new IllegalArgumentException("Source should contain zk nodes and path to properties");
        }
        String nodes = sourcePath.substring(nodesStartIndex, pathStartIndex);
        String path = sourcePath.substring(pathStartIndex);

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
            return new ByteArrayInputStream(data.get());
        } finally {
            curatorClient.stop();
        }
    }
}
