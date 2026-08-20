package cn.floriax.qqbot.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 公开事件模型密封接口：所有已映射的 QQ 平台事件的根类型。
 * 业务侧通过 on&lt;T : BotEvent&gt; 订阅具体子类型。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
sealed interface BotEvent {
    /** 平台原始消息 id，用于被动回复；可能为 null。 */
    val rawId: String?
}

/**
 * 群 @ 机器人消息事件（GROUP_AT_MESSAGE_CREATE）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
data class GroupAtMessage(
    override val rawId: String?,
    val content: String,
    val authorId: String,
    val groupId: String,
    val timestamp: String,
) : BotEvent

/**
 * 群聊消息事件（GROUP_MESSAGE_CREATE，"接收所有消息"模式下群内每条消息都会推送）。
 * [mentionedBot] 标识本条消息是否 @ 了机器人，业务侧据此自行过滤。
 *
 * @author WangZhiYao
 * @since 2026/8/20
 */
@Serializable
data class GroupMessage(
    override val rawId: String?,
    val content: String,
    val authorId: String,
    val groupId: String,
    val timestamp: String,
    val mentionedBot: Boolean = false,
) : BotEvent

/**
 * 单聊（C2C）消息事件（C2C_MESSAGE_CREATE）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
data class C2cMessage(
    override val rawId: String?,
    val content: String,
    val authorId: String,
    val timestamp: String,
) : BotEvent

/**
 * 未识别或解析失败的事件：保留原始类型名与 data，便于排查与后续适配。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
data class UnknownEvent(
    override val rawId: String?,
    val type: String,
    val data: JsonObject,
) : BotEvent
