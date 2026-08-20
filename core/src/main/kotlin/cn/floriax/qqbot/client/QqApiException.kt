package cn.floriax.qqbot.client

/**
 * 获取 access token 失败（网络异常或 token 接口返回非 200）时抛出的异常。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class TokenFetchException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * QQ 开放平台 API 调用失败异常：err_code != 0 或 HTTP 非 2xx 时抛出。
 * 携带解析后的错误码枚举、原始码、平台 message、traceId 与 HTTP 状态码，便于定位问题。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqApiException(
    val errorCode: QqErrorCode,
    val rawCode: Int,
    val apiMessage: String,
    val traceId: String?,
    val httpStatus: Int?,
) : Exception("QQ API error $rawCode (${errorCode.name}): $apiMessage traceId=$traceId http=$httpStatus")
