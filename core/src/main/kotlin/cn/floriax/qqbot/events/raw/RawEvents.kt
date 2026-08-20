package cn.floriax.qqbot.events.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 消息发送者信息，镜像官方 webhook 载荷中的 author 字段。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class RawAuthor(
    @SerialName("user_openid") val userOpenid: String? = null,
    @SerialName("id_openid") val idOpenid: String? = null,
    @SerialName("member_openid") val memberOpenid: String? = null,
)

/**
 * 消息中 @ 的实体（官方 mentions 数组元素），bot=true 表示 @ 的是机器人。
 *
 * @author WangZhiYao
 * @since 2026/8/20
 */
@Serializable
internal data class RawMention(
    val id: String? = null,
    @SerialName("member_openid") val memberOpenid: String? = null,
    val username: String? = null,
    val bot: Boolean? = null,
)

/**
 * 群 @ 消息原始结构，1:1 镜射官方 payload 的 d 字段，供 EventMapper 转换。
 * GROUP_AT_MESSAGE_CREATE 与 GROUP_MESSAGE_CREATE 共用此结构。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class RawGroupAtMessage(
    val id: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    val author: RawAuthor? = null,
    @SerialName("group_openid") val groupOpenid: String? = null,
    val mentions: List<RawMention> = emptyList(),
)

/**
 * 单聊消息原始结构，1:1 镜射官方 payload 的 d 字段，供 EventMapper 转换。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class RawC2cMessage(
    val id: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    val author: RawAuthor? = null,
)
