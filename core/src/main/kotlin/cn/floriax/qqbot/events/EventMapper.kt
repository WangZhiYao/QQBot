package cn.floriax.qqbot.events

import cn.floriax.qqbot.bus.EventPayload
import cn.floriax.qqbot.events.raw.RawC2cMessage
import cn.floriax.qqbot.events.raw.RawGroupAtMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * 解码管线：d: JsonObject → Raw*（镜像官方结构）→ 公开事件（拍平嵌套）。
 * 公开模型与官方 JSON 字段的偏差收敛在这一层。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal object EventMapper {

    private val json = Json { ignoreUnknownKeys = true }

    /** content 中 @ 实体的原文标记（如 `<@1D55…>`），全量消息模式下未剥离。 */
    private val mentionTagRegex = Regex("""<@[^>]*>""")

    /** 剥离 content 中的 @ 标记并修剪首尾空白。 */
    private fun stripMentionTags(content: String): String =
        content.replace(mentionTagRegex, "").trim()

    /** 将原始 payload 映射为公开事件；类型缺失或解析失败时降级为 UnknownEvent。 */
    fun map(payload: EventPayload): BotEvent {
        val t = payload.t ?: return UnknownEvent(payload.id, "NULL_TYPE", JsonObject(emptyMap()))
        val d = payload.d ?: return UnknownEvent(payload.id, t, JsonObject(emptyMap()))
        return try {
            when (t) {
                "GROUP_AT_MESSAGE_CREATE" -> {
                    val raw = json.decodeFromJsonElement(RawGroupAtMessage.serializer(), d)
                    GroupAtMessage(
                        rawId = raw.id,
                        content = raw.content.orEmpty(),
                        authorId = raw.author?.memberOpenid.orEmpty(),
                        groupId = raw.groupOpenid.orEmpty(),
                        timestamp = raw.timestamp.orEmpty(),
                    )
                }

                "GROUP_MESSAGE_CREATE" -> {
                    val raw = json.decodeFromJsonElement(RawGroupAtMessage.serializer(), d)
                    GroupMessage(
                        rawId = raw.id,
                        content = stripMentionTags(raw.content.orEmpty()),
                        authorId = raw.author?.memberOpenid.orEmpty(),
                        groupId = raw.groupOpenid.orEmpty(),
                        timestamp = raw.timestamp.orEmpty(),
                        mentionedBot = raw.mentions.any { it.bot == true },
                    )
                }

                "C2C_MESSAGE_CREATE" -> {
                    val raw = json.decodeFromJsonElement(RawC2cMessage.serializer(), d)
                    C2cMessage(
                        rawId = raw.id,
                        content = raw.content.orEmpty(),
                        authorId = raw.author?.userOpenid ?: raw.author?.idOpenid.orEmpty(),
                        timestamp = raw.timestamp.orEmpty(),
                    )
                }

                else -> UnknownEvent(payload.id, t, d)
            }
        } catch (e: IllegalArgumentException) {
            UnknownEvent(payload.id, t, d)
        }
    }
}
