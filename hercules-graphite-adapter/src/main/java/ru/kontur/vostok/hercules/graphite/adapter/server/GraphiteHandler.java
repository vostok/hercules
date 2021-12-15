package ru.kontur.vostok.hercules.graphite.adapter.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.Purgatory;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;

/**
 * The graphite handler processes metrics one by one.
 * <p>
 * Read metric from the buffer and pass it to the {@link Purgatory purgatory}.
 *
 * @author Gregory Koshelev
 */
@ChannelHandler.Sharable
public class GraphiteHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteHandler.class);
    private static final String METRICS_SCOPE = GraphiteHandler.class.getSimpleName();

    private final Purgatory purgatory;

    private final Meter unreadableMetricsMeter;

    public GraphiteHandler(Purgatory purgatory, MetricsCollector metricsCollector) {
        this.purgatory = purgatory;

        this.unreadableMetricsMeter = metricsCollector.meter(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "unreadableMetrics"));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;

        try {
            Metric metric = MetricReader.read(buf);
            if (metric != null) {
                purgatory.process(metric);
            } else {
                unreadableMetricsMeter.mark();
            }
        } finally {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Got exception", cause);
        ctx.close();
    }
}
