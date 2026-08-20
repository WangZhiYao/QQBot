package cn.floriax.qqbot

import cn.floriax.qqbot.bus.EventConsumer
import cn.floriax.qqbot.bus.EventDeduplicator
import cn.floriax.qqbot.bus.EventPayload
import cn.floriax.qqbot.client.HttpClientFactory
import cn.floriax.qqbot.client.QqBotClient
import cn.floriax.qqbot.scheduler.CoroutineBotScheduler
import cn.floriax.qqbot.webhook.SignaturePlugin
import cn.floriax.qqbot.webhook.webhookRoute
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/**
 * 安装 QQ 机器人框架：webhook 路由、验签、事件总线、OpenAPI client、调度器。
 * 配置优先级：DSL > application.yaml(qq.bot.*) > 环境变量(QQ_APP_ID/QQ_APP_SECRET)。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
/** 事件通道容量。 */
private const val EVENT_CHANNEL_CAPACITY = 1024

/** 去重表容量。 */
private const val DEDUP_CAPACITY = 4096

/** 去重记录保留时长（毫秒）。 */
private const val DEDUP_TTL_MILLIS = 600_000L

fun Application.qqBot(configure: QqBotConfigScope.() -> Unit): QqBot {
    val scope = QqBotConfigScope().apply(configure)
    val appId = scope.appId
        ?: environment.config.propertyOrNull("qq.bot.app-id")?.getString()
        ?: System.getenv("QQ_APP_ID")
        ?: error("appId 未配置（DSL/yaml/环境变量 QQ_APP_ID）")
    val appSecret = scope.appSecret
        ?: environment.config.propertyOrNull("qq.bot.app-secret")?.getString()
        ?: System.getenv("QQ_APP_SECRET")
        ?: error("appSecret 未配置（DSL/yaml/环境变量 QQ_APP_SECRET）")

    val qqClient = QqBotClient(
        appId,
        appSecret,
        HttpClientFactory.default()
    )
    val scheduler = scope.scheduler ?: CoroutineBotScheduler(
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    val channel =
        Channel<EventPayload>(EVENT_CHANNEL_CAPACITY)
    val dedup = EventDeduplicator(
        DEDUP_CAPACITY,
        DEDUP_TTL_MILLIS
    )
    val consumer = EventConsumer(channel, scope.registry)
    val bot0 = QqBot(qqClient, scheduler, channel, consumer) // reply() 需要引用
    consumer.contextWrapper =
        { block -> withContext(ReplyBotElement(bot0)) { block() } }
    val consumerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    consumer.start(consumerScope)

    install(SignaturePlugin) {
        this.appSecret = appSecret
        this.skip = scope.skipSignatureVerify
        this.pathPrefix = scope.webhookPath
    }
    routing {
        webhookRoute(channel, dedup, appSecret, scope.webhookPath)
    }

    val bot = bot0

    monitor.subscribe(ApplicationStopped) {
        consumer.stop()
        scheduler.shutdown()
        channel.close()
    }

    attributes.put(QqBotKey, bot)
    return bot
}

/**
 * 取回已安装的 QqBot 实例（须先调用 qqBot { } 安装）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
fun Application.qqBot(): QqBot = attributes[QqBotKey]
