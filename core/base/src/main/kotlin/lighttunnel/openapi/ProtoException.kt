package lighttunnel.openapi

class ProtoException constructor(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

