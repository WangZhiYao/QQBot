package cn.floriax.qqbot

import cn.floriax.qqbot.bus.EventPayload
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 测试钩子：直接向事件总线注入原始 payload（绕过 webhook/验签），
 * 仅供 core 测试源集使用，不进入发布构件。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal suspend fun QqBot.testDispatch(rawJson: String) {
    val payload = Json { ignoreUnknownKeys = true }
        .decodeFromString(EventPayload.serializer(), rawJson)
    withContext(ReplyBotElement(this)) {
        channel.send(payload)
    }
}

/** 测试钩子（同步版）：阻塞等待消费者处理完。 */
internal fun QqBot.testDispatchBlocking(rawJson: String) {
    runBlocking { testDispatch(rawJson) }
}
