package cn.floriax.qqbot

import cn.floriax.qqbot.events.C2cMessage
import cn.floriax.qqbot.events.GroupAtMessage
import cn.floriax.qqbot.events.GroupMessage
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

/**
 * reply 上下文元素：EventConsumer 调 handler 时注入，
 * 使 reply() 扩展能取到当前 QqBot 实例。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal class ReplyBotElement(val bot: QqBot) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    internal companion object Key : CoroutineContext.Key<ReplyBotElement>
}

/**
 * 事件处理器内直接回复（群 @ 消息）。
 * bot 实例由消费者协程上下文注入。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
suspend fun GroupAtMessage.reply(text: String) {
    val element = currentCoroutineContext()[ReplyBotElement]
        ?: error("reply() 只能在 on<T> 处理器内调用")
    element.bot.client.replyGroupMessage(groupId, text, rawId ?: "")
}

/**
 * 事件处理器内直接回复（单聊）。
 * bot 实例由消费者协程上下文注入。
 *
 * @author WangZhiYao
 * @since 2026/8/20
 */
suspend fun C2cMessage.reply(text: String) {
    val element = currentCoroutineContext()[ReplyBotElement]
        ?: error("reply() 只能在 on<T> 处理器内调用")
    element.bot.client.replyC2CMessage(authorId, text, rawId ?: "")
}

/**
 * 事件处理器内直接回复（GROUP_MESSAGE_CREATE 群消息，须先判断 mentionedBot）。
 * bot 实例由消费者协程上下文注入。
 *
 * @author WangZhiYao
 * @since 2026/8/20
 */
suspend fun GroupMessage.reply(text: String) {
    val element = currentCoroutineContext()[ReplyBotElement]
        ?: error("reply() 只能在 on<T> 处理器内调用")
    element.bot.client.replyGroupMessage(groupId, text, rawId ?: "")
}
