package cn.floriax.qqbot

import cn.floriax.qqbot.bus.EventConsumer
import cn.floriax.qqbot.bus.EventPayload
import cn.floriax.qqbot.client.QqBotClient
import cn.floriax.qqbot.scheduler.BotScheduler
import io.ktor.util.AttributeKey
import kotlinx.coroutines.channels.Channel

/**
 * 框 facade：聚合 OpenAPI client、调度器、事件通道与消费者，
 * 供业务侧调用 API 与注册定时任务。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqBot internal constructor(
    val client: QqBotClient,
    val scheduler: BotScheduler,
    internal val channel: Channel<EventPayload>,
    internal val consumer: EventConsumer,
)

/** Application attributes 中存放 QqBot 实例的键。 */
val QqBotKey = AttributeKey<QqBot>("qqbot-instance")
