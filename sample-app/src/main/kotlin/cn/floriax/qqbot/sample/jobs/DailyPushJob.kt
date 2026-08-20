package cn.floriax.qqbot.sample.jobs

import cn.floriax.qqbot.client.MsgSeqGenerator
import cn.floriax.qqbot.client.QqBotClient
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * 推送目标表（SQLite）。scene: "c2c" | "group"。
 * 以 openId + scene 联合主键标识一个推送目标。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
object PushTargets : Table("push_targets") {
    val openId = varchar("open_id", 64)
    val scene = varchar("scene", 16)
    override val primaryKey = PrimaryKey(openId, scene)
}

/**
 * 每日推送任务：查目标表 → 通过框架 client 发主动消息。
 * 按 scene 区分群聊与单聊通道，单个目标失败不影响其余目标。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class DailyPushJob(private val client: QqBotClient) {
    private val logger = LoggerFactory.getLogger(DailyPushJob::class.java)
    private val seq = MsgSeqGenerator()

    /** 新增推送目标。 */
    fun addTarget(openId: String, scene: String) {
        transaction {
            PushTargets.insert {
                it[PushTargets.openId] = openId
                it[PushTargets.scene] = scene
            }
        }
    }

    /** 向所有目标推送文本，失败仅记录日志。 */
    suspend fun pushDaily(text: String = "早安推送") {
        val targets = transaction {
            PushTargets.selectAll().map { it[PushTargets.openId] to it[PushTargets.scene] }
        }
        for ((openId, scene) in targets) {
            runCatching {
                if (scene == "group") client.sendGroupMessage(openId, text, seq.next(openId))
                else client.sendC2C(openId, text, seq.next(openId))
            }.onFailure { logger.error("push to {} failed", openId, it) }
        }
    }
}
