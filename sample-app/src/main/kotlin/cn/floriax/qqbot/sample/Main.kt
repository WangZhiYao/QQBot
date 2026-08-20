package cn.floriax.qqbot.sample

import cn.floriax.qqbot.events.C2cMessage
import cn.floriax.qqbot.events.GroupAtMessage
import cn.floriax.qqbot.events.GroupMessage
import cn.floriax.qqbot.qqBot
import cn.floriax.qqbot.reply
import cn.floriax.qqbot.sample.db.SampleDb
import cn.floriax.qqbot.sample.jobs.DailyPushJob
import cn.floriax.qqbot.sample.jobs.QuartzBotScheduler
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*

/**
 * 示例应用模块：初始化数据库、装配 Quartz 调度器与 QQ 机器人，
 * 注册群 @ 消息与单聊消息的回复逻辑，并配置每日 9 点的定时推送。
 * 由 EngineMain 按 application.yaml（ktor.application.modules）加载。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
fun Application.module() {
    install(CallLogging)
    SampleDb.init()

    val quartz = QuartzBotScheduler()
    val bot = qqBot {
        // 优先环境变量，回落 application.yaml（qq.bot.*）
        appId = System.getenv("QQ_APP_ID") ?: environment.config.property("qq.bot.app-id").getString()
        appSecret = System.getenv("QQ_APP_SECRET") ?: environment.config.property("qq.bot.app-secret").getString()
        webhookPath = "/qq/webhook"
        scheduler = quartz

        on<GroupAtMessage> {
            reply("收到: ${content.trim()}")
        }
        on<GroupMessage> {
            // GROUP_MESSAGE_CREATE 会推送群内所有消息，只响应 @ 机器人的
            if (mentionedBot) reply("收到: ${content.trim()}")
        }
        on<C2cMessage> {
            reply("你好！")
        }
    }

    // 每日 9 点推送
    val push = DailyPushJob(bot.client)
    quartz.schedule("0 0 9 * * ?", "daily-push") { push.pushDaily("早安推送") }
    quartz.start()
}
