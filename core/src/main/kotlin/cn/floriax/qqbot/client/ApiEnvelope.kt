package cn.floriax.qqbot.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * QQ OpenAPI 响应公共信封：err_code、message 与 trace_id。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class ApiEnvelope(
    @SerialName("err_code") val errCode: Int = 0,
    val message: String = "",
    @SerialName("trace_id") val traceId: String? = null,
)
