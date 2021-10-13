package ru.kontur.vostok.hercules.graphite.adapter.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.timeout.ReadTimeoutHandler;
import ru.kontur.vostok.hercules.graphite.adapter.GraphiteAdapterDefaults;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.Purgatory;
import ru.kontur.vostok.hercules.graphite.adapter.util.NettyUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * The {@link GraphiteAdapterServer} is a Graphite-compatible TCP-sever.
 * <p>
 * Supports the plaintext Graphite format. Also, supports metrics with tags.
 * <p>
 * Supported format is:
 * <pre>
 *     [{metric-prefix}.]{metric-name}[;{tag-key}={tag-value}]* {value} {timestamp}
 * </pre>
 * Here, {@code [{metric-prefix}.]} is an optional dot-separated metric prefix,
 * {@code [;{tag-key}={tag-value}]*} is a set of metric tags.
 *
 * @author Gregory Koshelev
 */
public class GraphiteAdapterServer implements Lifecycle {
    private final String host;
    private final int port;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerBootstrap bootstrap;

    public GraphiteAdapterServer(Properties properties, Purgatory purgatory, MetricsCollector metricsCollector) {
        host = PropertiesUtil.get(Props.HOST, properties).get();
        port = PropertiesUtil.get(Props.PORT, properties).get();

        int workerThreadCount = PropertiesUtil.get(Props.WORKER_THREAD_COUNT, properties).get();
        int readTimeoutMs = PropertiesUtil.get(Props.READ_TIMEOUT_MS, properties).get();
        Integer recvBufferSizeBytes = PropertiesUtil.get(Props.RECV_BUFFER_SIZE_BYTES, properties).orEmpty(null);

        bossGroup = new NioEventLoopGroup(
                1,
                ThreadFactories.newNamedThreadFactory("bossEventLoop", false));
        workerGroup = new NioEventLoopGroup(
                workerThreadCount,
                ThreadFactories.newNamedThreadFactory("workerEventLoop", false));

        GraphiteHandler graphiteHandler = new GraphiteHandler(purgatory, metricsCollector);

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) {
                                channel.pipeline().
                                        /* Wait for new metrics for this period of time */
                                        addLast("readTimeout", new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)).
                                        /* One metric per line, a metric length must not exceed 1024 bytes */
                                        addLast("decoder", new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter())).
                                        /* Process metrics one by one. Reuse handler on all connections */
                                        addLast("graphiteHandler", graphiteHandler);
                            }
                        })
                /* Queueing new connections during the storm at GA restart */
                .option(ChannelOption.SO_BACKLOG, 2048)
                /* Persistent connections */
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                /* Avoid lack of ports on the server with balancing proxy due to TIME_WAIT at GA restart */
                .childOption(ChannelOption.SO_LINGER, 0)
                /* Use system default if not set */
                .childOption(ChannelOption.SO_RCVBUF, recvBufferSizeBytes);
    }

    @Override
    public void start() {
        try {
            bootstrap.bind(host, port).sync();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            NettyUtil.syncAll(workerGroup.shutdownGracefully(), bossGroup.shutdownGracefully());//TODO: use timeout
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    private static class Props {
        static final Parameter<String> HOST =
                Parameter.stringParameter("host").
                        withDefault(GraphiteAdapterDefaults.DEFAULT_HOST).
                        build();

        static final Parameter<Integer> PORT =
                Parameter.integerParameter("port").
                        withDefault(GraphiteAdapterDefaults.DEFAULT_PORT).
                        withValidator(IntegerValidators.portValidator()).
                        build();

        /**
         * Worker thread count.
         *
         * Default to {@code CPU cores x 2}
         */
        static final Parameter<Integer> WORKER_THREAD_COUNT =
                Parameter.integerParameter("worker.thread.count").
                        withDefault(Runtime.getRuntime().availableProcessors() * 2).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<Integer> READ_TIMEOUT_MS =
                Parameter.integerParameter("read.timeout.ms").
                        withDefault(GraphiteAdapterDefaults.DEFAULT_READ_TIMEOUT_MS).
                        withValidator(IntegerValidators.positive()).
                        build();

        static final Parameter<Integer> RECV_BUFFER_SIZE_BYTES =
                Parameter.integerParameter("recv.buffer.size.bytes").
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}