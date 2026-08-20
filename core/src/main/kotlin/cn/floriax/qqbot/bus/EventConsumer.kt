package cn.floriax.qqbot.bus

import cn.floriax.qqbot.events.BotEvent
import cn.floriax.qqbot.events.EventMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/** 挂起事件处理器。 */
internal typealias Handler = suspend (BotEvent) -> Unit

/**
 * 事件处理器注册表：按事件的 KClass 查表分发。
 * 消费语义：串行——单个慢 handler 会延迟后续事件（保证 handler 内无并发顾虑）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@PublishedApi
internal class EventHandlerRegistry {
    private val handlers = LinkedHashMap<KClass<out BotEvent>, MutableList<Handler>>()

    /** 注册指定事件类型的挂起 handler。 */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : BotEvent> on(
        clazz: KClass<out T>,
        handler: suspend (T) -> Unit,
    ) {
        handlers.getOrPut(clazz) { mutableListOf() }
            .add(handler as Handler)
    }

    /** 返回该事件类型注册的全部 handler（精确匹配运行时 KClass，无父类匹配）。 */
    fun handlersFor(event: BotEvent): List<Handler> =
        handlers[event::class].orEmpty()
}

/**
 * 长驻消费者：Channel → EventMapper 解码 → 按类型分发。
 * 用户 handler 异常被捕获记 ERROR，不影响消费者存活与其他订阅者。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal class EventConsumer(
    private val channel: Channel<EventPayload>,
    private val registry: EventHandlerRegistry,
) {
    private val logger: Logger = LoggerFactory.getLogger(EventConsumer::class.java)
    private var job: Job? = null

    /** reply() 上下文注入器：默认无（生产由 QqBotPlugin 提供）。 */
    internal var contextWrapper: suspend (suspend () -> Unit) -> Unit = { it() }

    /** 在指定作用域启动消费协程，循环从 Channel 取出事件并分发。 */
    fun start(scope: CoroutineScope) {
        job = scope.launch {
            for (payload in channel) {
                val event = EventMapper.map(payload)
                val handlers = registry.handlersFor(event)
                if (handlers.isEmpty()) {
                    logger.debug("no subscriber for event t={} id={}", payload.t, payload.id)
                    continue
                }
                for (h in handlers) {
                    contextWrapper {
                        runCatching { h(event) }
                            .onFailure { logger.error("handler failed for event id={}", payload.id, it) }
                    }
                }
            }
        }
    }

    /** 挂起等待消费协程结束（主要用于测试）。 */
    suspend fun await() {
        job?.join()
    }

    /** 取消消费协程。 */
    fun stop() {
        job?.cancel()
    }
}
