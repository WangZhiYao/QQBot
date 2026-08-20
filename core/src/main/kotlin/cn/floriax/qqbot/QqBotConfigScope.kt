package cn.floriax.qqbot

import cn.floriax.qqbot.bus.EventHandlerRegistry

/**
 * qqBot { } 配置作用域：提供凭证、验签开关、调度器覆盖与事件处理器注册的 DSL 入口。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqBotConfigScope {
    /** 机器人 AppID；未设置时回退到 application.yaml / 环境变量。 */
    var appId: String? = null

    /** AppSecret；未设置时回退到 application.yaml / 环境变量。 */
    var appSecret: String? = null

    /** 是否跳过 webhook 签名校验（仅本地调试用）。 */
    var skipSignatureVerify: Boolean = false

    /** webhook 接收路径；默认 /qq/webhook，可按管理端配置调整。 */
    var webhookPath: String = "/qq/webhook"

    /** 调度器覆盖（如 Quartz 适配）；null = 默认协程实现。 */
    var scheduler: cn.floriax.qqbot.scheduler.BotScheduler? = null

    @PublishedApi
    internal val registry = EventHandlerRegistry()

    /** 注册指定事件类型的挂起处理器。 */
    inline fun <reified T : cn.floriax.qqbot.events.BotEvent> on(noinline handler: suspend T.() -> Unit) {
        registry.on(T::class) { event -> handler(event) }
    }
}
