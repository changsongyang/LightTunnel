package lighttunnel.base.entity

import lighttunnel.base.generated.GeneratedBuildConfig
import lighttunnel.base.utils.base64Decode

object BuildConfig {
    const val PROTO_VERSION = lighttunnel.base.proto.Proto.VERSION
    const val VERSION_CODE = GeneratedBuildConfig.VERSION_CODE
    const val VERSION_NAME = GeneratedBuildConfig.VERSION_NAME
    const val LAST_COMMIT_SHA = GeneratedBuildConfig.LAST_COMMIT_SHA
    const val LAST_COMMIT_DATE = GeneratedBuildConfig.LAST_COMMIT_DATE
    const val BUILD_DATA = GeneratedBuildConfig.BUILD_DATA
    val BUILTIN_SERVER_JKS_BYTES = base64Decode(GeneratedBuildConfig.BUILTIN_SERVER_JKS_BASE64)
    const val BUILTIN_SERVER_JKS_STORE_PASSWORD = GeneratedBuildConfig.BUILTIN_SERVER_JKS_STORE_PASSWORD
    const val BUILTIN_SERVER_JKS_KEY_PASSWORD = GeneratedBuildConfig.BUILTIN_SERVER_JKS_KEY_PASSWORD
    val BUILTIN_CLIENT_JKS_BYTES = base64Decode(GeneratedBuildConfig.BUILTIN_CLIENT_JKS_BASE64)
    const val BUILTIN_CLIENT_JKS_STORE_PASSWORD = GeneratedBuildConfig.BUILTIN_CLIENT_JKS_STORE_PASSWORD
}
