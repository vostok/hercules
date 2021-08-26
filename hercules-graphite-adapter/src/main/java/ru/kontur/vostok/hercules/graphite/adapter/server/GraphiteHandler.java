package ru.kontur.vostok.hercules.graphite.adapter.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.Purgatory;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;

/**
 * The graphite handler processes metrics one by one.
 * <p>
 * Read metric from the buffer and pass it to the {@link Purgatory purgatory}.
 *
 * @author Gregory Koshelev
 */
public class GraphiteHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteHandler.class);

    private final Purgatory purgatory;

    public GraphiteHandler(Purgatory purgatory) {
        this.purgatory = purgatory;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;

        Metric metric = MetricReader.read(buf);
        if (metric != null) {
            purgatory.process(metric);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Got exception", cause);
        ctx.close();
    }
}
