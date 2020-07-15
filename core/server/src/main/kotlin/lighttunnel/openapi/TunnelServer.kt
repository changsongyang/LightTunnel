@file:Suppress("unused")

package lighttunnel.openapi

import lighttunnel.openapi.args.*
import lighttunnel.openapi.http.HttpFd
import lighttunnel.openapi.listener.OnHttpTunnelStateListener
import lighttunnel.openapi.listener.OnTcpTunnelStateListener
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
    httpRpcServerArgs: HttpRpcServerArgs? = null,
    onTcpTunnelStateListener: OnTcpTunnelStateListener? = null,
    onHttpTunnelStateListener: OnHttpTunnelStateListener? = null
) {
    private val daemon by lazy {
        TunnelServerDaemon(
            bossThreads = bossThreads,
            workerThreads = workerThreads,
            tunnelDaemonArgs = tunnelDaemonArgs,
            sslTunnelDaemonArgs = sslTunnelDaemonArgs,
            httpTunnelArgs = httpTunnelArgs,
            httpsTunnelArgs = httpsTunnelArgs,
            httpRpcServerArgs = httpRpcServerArgs,
            onTcpTunnelStateListener = onTcpTunnelStateListener,
            onHttpTunnelStateListener = onHttpTunnelStateListener
        )
    }

    @Throws(Exception::class)
    fun start(): Unit = daemon.start()
    fun depose(): Unit = daemon.depose()
    fun getTcpFdList(): List<TcpFd> = daemon.tcpRegistry.tcpFds()
    fun getHttpFdList(): List<HttpFd> = daemon.httpRegistry.httpFds()
    fun getHttpsFdList(): List<HttpFd> = daemon.httpsRegistry.httpFds()
    fun forceOff(fd: TcpFd): TcpFd? = daemon.tcpRegistry.forceOff((fd as TcpFdDefaultImpl).port)
    fun forceOff(fd: HttpFd): HttpFd? = (if (fd.isHttps) daemon.httpsRegistry else daemon.httpRegistry).forceOff((fd as HttpFdDefaultImpl).host)
    fun isTcpRegistered(port: Int) = daemon.tcpRegistry.isRegistered(port)
    fun isHttpRegistered(host: String) = daemon.httpRegistry.isRegistered(host)
    fun isHttpsRegistered(host: String) = daemon.httpsRegistry.isRegistered(host)


}