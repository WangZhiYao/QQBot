package cn.floriax.qqbot.client

import java.util.concurrent.ConcurrentHashMap

/**
 * 主动消息 msg_seq 递增工具：同一 (群/用户) openId 维度从 1 递增。
 * QQ 开放平台要求主动消息携带递增的 msg_seq，用于去重与限频统计。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class MsgSeqGenerator {
    private val counters = ConcurrentHashMap<String, UInt>()

    /** 返回指定 openId 维度的下一个 msg_seq（首个为 1），线程安全。 */
    fun next(openId: String): UInt =
        counters.compute(openId) { _, v -> if (v == null) 1u else v + 1u } ?: 1u
}
