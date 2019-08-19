package com.tuuzed.tunnel.webframework;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpWebServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpWebServer.class);

    @NotNull
    private final HttpRouter router = new HttpRouter();
    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @Nullable
    private final String bindAddr;
    private final int bindPort;
    private int maxContentLength;

    public HttpWebServer(
        @NotNull NioEventLoopGroup bossGroup,
        @NotNull NioEventLoopGroup workerGroup
    ) {
        this(
            bossGroup,
            workerGroup,
            null,
            8080,
            1024 * 1024 // 1MB
        );
    }

    public HttpWebServer(
        @NotNull NioEventLoopGroup bossGroup,
        @NotNull NioEventLoopGroup workerGroup,
        @Nullable String bindAddr,
        int bindPort,
        int maxContentLength
    ) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.bindAddr = bindAddr;
        this.bindPort = bindPort;
        this.maxContentLength = maxContentLength;
    }

    @NotNull
    public HttpWebServer routing(@NotNull String path, @NotNull HttpRequestHandler handler) {
        router.routing(path, handler);
        return this;
    }

    public void start() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new HttpWebServerChannelInitializer(maxContentLength, router));
        if (bindAddr == null) {
            bootstrap.bind(bindPort).get();
            logger.info("Serving Http on any address port {}", bindPort);
        } else {
            bootstrap.bind(bindAddr, bindPort).get();
            logger.info("Serving Http on {} port {}", bindAddr, bindPort);
        }
    }

}
