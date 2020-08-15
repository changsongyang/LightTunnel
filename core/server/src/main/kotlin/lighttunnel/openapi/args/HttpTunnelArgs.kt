package lighttunnel.openapi.args

import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor

class HttpTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 80,
    val maxContentLength: Int = 1024 * 1024 * 8,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
)