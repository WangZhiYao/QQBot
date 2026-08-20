package cn.floriax.qqbot.bus

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * QQ 平台 Webhook 回调的原始载荷（协议帧的精简映射）。
 * 字段与官方文档一致：op 为操作码，t 为事件类型，d 为事件数据。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@Serializable
internal data class EventPayload(
    /** 事件唯一 id，用于去重；op=13 校验包中可能缺失。 */
    val id: String? = null,
    /** 操作码：0=事件分发，13=回调地址校验。 */
    val op: Int = -1,
    /** 事件序号（平台侧单调递增），用于排序/补偿。 */
    val s: Long? = null,
    /** 事件类型名，如 C2C_MESSAGE_CREATE。 */
    val t: String? = null,
    /** 事件数据体，结构随事件类型变化。 */
    val d: JsonObject? = null,
)
