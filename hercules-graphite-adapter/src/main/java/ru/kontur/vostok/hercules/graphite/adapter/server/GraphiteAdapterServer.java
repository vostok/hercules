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
    private final int port;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerBootstrap bootstrap;

    public GraphiteAdapterServer(Properties properties, Purgatory purgatory, MetricsCollector metricsCollector) {
        port = PropertiesUtil.get(Props.PORT, properties).get();
        int readTimeoutMs = PropertiesUtil.get(Props.READ_TIMEOUT_MS, properties).get();

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) {
                                channel.pipeline().
                                        addLast("readTimeout", new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)).
                                        addLast("decoder", new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter())).
                                        addLast("graphiteHandler", new GraphiteHandler(purgatory));
                            }
                        })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    @Override
    public void start() {
        try {
            bootstrap.bind(port).sync();
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
        static final Parameter<Integer> PORT =
                Parameter.integerParameter("port").
                        withDefault(GraphiteAdapterDefaults.DEFAULT_PORT).
                        withValidator(IntegerValidators.portValidator()).
                        build();

        static final Parameter<Integer> READ_TIMEOUT_MS =
                Parameter.integerParameter("read.timeout.ms").
                        withDefault(GraphiteAdapterDefaults.DEFAULT_READ_TIMEOUT_MS).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}