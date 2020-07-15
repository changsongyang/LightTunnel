package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import lighttunnel.base.http.server.HttpServer
import lighttunnel.base.logger.loggerDelegate
import lighttunnel.base.proto.HeartbeatHandler
import lighttunnel.base.proto.ProtoMessageDecoder
import lighttunnel.base.proto.ProtoMessageEncoder
import lighttunnel.base.util.IncIds
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.openapi.args.*
import lighttunnel.openapi.listener.OnHttpTunnelStateListener
import lighttunnel.openapi.listener.OnTcpTunnelStateListener
import lighttunnel.server.http.HttpFdDefaultImpl
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpFdDefaultImpl
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.traffic.TrafficHandler
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TunnelServerDaemon(
    bossThreads: Int,
    workerThreads: Int,
    private val tunnelDaemonArgs: TunnelDaemonArgs,
    private val sslTunnelDaemonArgs: SslTunnelDaemonArgs?,
    httpTunnelArgs: HttpTunnelArgs?,
    httpsTunnelArgs: HttpsTunnelArgs?,
    httpRpcServerArgs: HttpRpcServerArgs?,
    private val onTcpTunnelStateListener: OnTcpTunnelStateListener?,
    private val onHttpTunnelStateListener: OnHttpTunnelStateListener?
) {
    private val logger by loggerDelegate()
    private val lock = ReentrantLock()
    private val tunnelIds = IncIds()

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    val tcpRegistry: TcpRegistry = TcpRegistry()
    val httpRegistry: HttpRegistry = HttpRegistry()
    val httpsRegistry: HttpRegistry = HttpRegistry()

    private val tcpTunnel: TcpTunnel = getTcpTunnel(tcpRegistry)
    private val httpTunnel: HttpTunnel? = httpTunnelArgs?.let { getHttpTunnel(httpRegistry, it) }
    private val httpsTunnel: HttpTunnel? = httpsTunnelArgs?.let { getHttpsTunnel(httpsRegistry, it) }
    private val httpRpcServer: HttpServer? = httpRpcServerArgs?.let { getHttpRpcServer(it) }

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        httpTunnel?.start()
        httpsTunnel?.start()
        httpRpcServer?.start()
        startTunnelDaemon(tunnelDaemonArgs)
        sslTunnelDaemonArgs?.also { startSslTunnelDaemon(it) }
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry.depose()
        httpRegistry.depose()
        httpsRegistry.depose()
        httpRpcServer?.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun startTunnelDaemon(args: TunnelDaemonArgs) {
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addLast("traffic", TrafficHandler())
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", InnerTunnelServerChannelHandler(args.tunnelRequestInterceptor))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun startSslTunnelDaemon(args: SslTunnelDaemonArgs) {
        if (args.bindPort == null) {
            return
        }
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addFirst("ssl", args.sslContext.newHandler(ch.alloc()))
                        .addLast("traffic", TrafficHandler())
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", InnerTunnelServerChannelHandler(args.tunnelRequestInterceptor))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel with ssl on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun getTcpTunnel(registry: TcpRegistry): TcpTunnel {
        return TcpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = registry
        )
    }

    private fun getHttpTunnel(registry: HttpRegistry, args: HttpTunnelArgs): HttpTunnel? {
        if (args.bindPort == null) {
            return null
        }
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = null,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun getHttpsTunnel(registry: HttpRegistry, args: HttpsTunnelArgs): HttpTunnel? {
        if (args.bindPort == null) {
            return null
        }
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = args.sslContext,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun getHttpRpcServer(args: HttpRpcServerArgs): HttpServer? {
        if (args.bindPort == null) {
            return null
        }
        return HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort
        ) {
            route("/api/version") {
                val content = JSONObject().apply {
                    put("version", BuildConfig.VERSION_NAME)
                }.let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
                ).apply {
                    headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                }
            }
            route("/api/snapshot") {
                val content = JSONObject().apply {
                    put("tcp", tcpRegistry.toJson())
                    put("http", httpRegistry.toJson())
                    put("https", httpsRegistry.toJson())
                }.let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
                ).apply {
                    headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                }
            }
        }
    }

    private inner class InnerTunnelServerChannelHandler(
        tunnelRequestInterceptor: TunnelRequestInterceptor
    ) : TunnelServerDaemonChannelHandler(
        tunnelRequestInterceptor = tunnelRequestInterceptor,
        tunnelIds = tunnelIds,
        tcpTunnel = tcpTunnel,
        httpTunnel = httpTunnel,
        httpsTunnel = httpsTunnel
    ) {
        override fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?) {
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onTcpTunnelConnected(tcpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?) {
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onTcpTunnelDisconnect(tcpFd)
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?) {
            if (httpFd != null) {
                onHttpTunnelStateListener?.onHttpTunnelConnected(httpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?) {
            if (httpFd != null) {
                onHttpTunnelStateListener?.onHttpTunnelDisconnect(httpFd)
            }
        }
    }


}