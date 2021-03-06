@file:Suppress("unused")

package lighttunnel.openapi

import lighttunnel.openapi.args.HttpTunnelArgs
import lighttunnel.openapi.args.HttpsTunnelArgs
import lighttunnel.openapi.args.SslTunnelDaemonArgs
import lighttunnel.openapi.args.TunnelDaemonArgs
import lighttunnel.openapi.http.HttpFd
import lighttunnel.openapi.listener.OnHttpTunnelStateListener
import lighttunnel.openapi.listener.OnTcpTunnelStateListener
import lighttunnel.openapi.listener.OnTrafficListener
import lighttunnel.openapi.tcp.TcpFd
import lighttunnel.server.TunnelServerDaemon
import lighttunnel.server.http.HttpFdDefaultImpl
import lighttunnel.server.tcp.TcpFdDefaultImpl

class TunnelServer(
    bossThreads: Int = -1,
    workerThreads: Int = -1,
    tunnelDaemonArgs: TunnelDaemonArgs = TunnelDaemonArgs(),
    sslTunnelDaemonArgs: SslTunnelDaemonArgs? = null,
    httpTunnelArgs: HttpTunnelArgs? = null,
    httpsTunnelArgs: HttpsTunnelArgs? = null,
    isHttpAndHttpsShareRegistry: Boolean = false,
    onTcpTunnelStateListener: OnTcpTunnelStateListener? = null,
    onHttpTunnelStateListener: OnHttpTunnelStateListener? = null,
    onTrafficListener: OnTrafficListener? = null
) {
    private val daemon by lazy {
        TunnelServerDaemon(
            bossThreads = bossThreads,
            workerThreads = workerThreads,
            tunnelDaemonArgs = tunnelDaemonArgs,
            sslTunnelDaemonArgs = sslTunnelDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs,
            isHttpAndHttpsShareRegistry = isHttpAndHttpsShareRegistry,
            onTcpTunnelStateListener = onTcpTunnelStateListener,
            onHttpTunnelStateListener = onHttpTunnelStateListener,
            onTrafficListener = onTrafficListener
        )
    }

    val isSupportSsl = sslTunnelDaemonArgs != null
    val isSupportHttp = httpTunnelArgs != null
    val isSupportHttps = httpsTunnelArgs != null

    val httpPort = httpTunnelArgs?.bindPort
    val httpsPort = httpsTunnelArgs?.bindPort

    @Throws(Exception::class)
    fun start(): Unit = daemon.start()
    fun depose(): Unit = daemon.depose()
    fun getTcpFd(port: Int): TcpFd? = daemon.tcpRegistry.getTcpFd(port)
    fun getHttpFd(host: String): HttpFd? = daemon.httpRegistry.getHttpFd(host)
    fun getHttpsFd(host: String): HttpFd? = daemon.httpsRegistry.getHttpFd(host)
    fun getTcpFdList(): List<TcpFd> = daemon.tcpRegistry.getTcpFdList()
    fun getHttpFdList(): List<HttpFd> = daemon.httpRegistry.getHttpFdList()
    fun getHttpsFdList(): List<HttpFd> = daemon.httpsRegistry.getHttpFdList()
    fun forceOff(fd: TcpFd): TcpFd? = daemon.tcpRegistry.forceOff((fd as TcpFdDefaultImpl).port)
    fun forceOff(fd: HttpFd): HttpFd? = (if (fd.isHttps) daemon.httpsRegistry else daemon.httpRegistry).forceOff((fd as HttpFdDefaultImpl).host)
    fun isTcpRegistered(port: Int) = daemon.tcpRegistry.isRegistered(port)
    fun isHttpRegistered(host: String) = daemon.httpRegistry.isRegistered(host)
    fun isHttpsRegistered(host: String) = daemon.httpsRegistry.isRegistered(host)


}