package lighttunnel.server.utils

import io.netty.util.AttributeKey
import lighttunnel.common.proto.Proto
import lighttunnel.server.SessionChannels
import lighttunnel.server.TunnelDescriptor
import lighttunnel.server.http.DefaultHttpContext

private const val PREFIX = "\$lighttunnel.server"

internal val AK_AES128_KEY get() = Proto.AK_AES128_KEY

internal val AK_SESSION_ID = AttributeKey.newInstance<Long?>("$PREFIX.SessionId")

internal val AK_SESSION_CHANNELS = AttributeKey.newInstance<SessionChannels?>("$PREFIX.SessionChannels")

internal val AK_TUNNEL_DESCRIPTOR = AttributeKey.newInstance<TunnelDescriptor?>("$PREFIX.TunnelDescriptor")

internal val AK_WATCHDOG_TIME_MILLIS = AttributeKey.newInstance<Long?>("$PREFIX.WatchdogTimeMillis")

internal val AK_HTTP_HOST = AttributeKey.newInstance<String?>("$PREFIX.HttpHost")

internal val AK_IS_PLUGIN_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isPluginHandle")

internal val AK_IS_INTERCEPTOR_HANDLE = AttributeKey.newInstance<Boolean?>("$PREFIX.isInterceptorHandle")

internal val AK_HTTP_CONTEXT = AttributeKey.newInstance<DefaultHttpContext?>("$PREFIX.HttpContext")