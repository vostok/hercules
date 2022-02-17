package ru.kontur.vostok.hercules.graphite.adapter.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
@ChannelHandler.Sharable
public class ConnectionLimiter extends ChannelInboundHandlerAdapter {
    private final int connLimit;
    private final AtomicInteger connections = new AtomicInteger();

    /**
     * {@link ConnectionLimiter} limits concurrent connections
     *
     * @param connLimit connection limit
     */
    public ConnectionLimiter(int connLimit) {
        this.connLimit = connLimit;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (connections.incrementAndGet() > connLimit) {
            ctx.close();
        } else {
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        connections.decrementAndGet();
    }
}
