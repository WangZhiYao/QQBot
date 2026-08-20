package cn.floriax.qqbot.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * token 接口（/app/getAppAccessToken）响应体。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)
