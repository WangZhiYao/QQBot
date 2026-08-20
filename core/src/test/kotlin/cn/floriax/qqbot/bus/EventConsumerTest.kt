package cn.floriax.qqbot.bus

import cn.floriax.qqbot.events.GroupAtMessage
import cn.floriax.qqbot.events.UnknownEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * EventConsumer 事件消费测试：覆盖按类型分发到订阅者、handler 异常不终止消费循环、
 * 解码失败降级为 UnknownEvent 以及无订阅者时正常处理不抛异常。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class EventConsumerTest {

    /** 群 @ 消息事件的测试 payload JSON 固件。 */
    private val groupPayloadJson = """
        {"id":"evt-9","op":0,"t":"GROUP_AT_MESSAGE_CREATE",
         "d":{"id":"m9","content":"hi","author":{"member_openid":"u1"},"group_openid":"g1",
              "timestamp":"2026-08-19T10:00:00+08:00"}}
    """.trimIndent()

    @Test
    fun `dispatches to subscriber of matching type`() = runTest {
        val channel = Channel<EventPayload>(8)
        val registry = EventHandlerRegistry()
        val received = mutableListOf<GroupAtMessage>()
        registry.on(GroupAtMessage::class) { received.add(it) }
        val consumer = EventConsumer(channel, registry)
        consumer.start(this)
        channel.send(Json.decodeFromString(EventPayload.serializer(), groupPayloadJson))
        channel.close()
        consumer.await()
        assertEquals(1, received.size)
        assertEquals("hi", received[0].content)
    }

    @Test
    fun `handler exception does not kill consumer`() = runTest {
        val channel = Channel<EventPayload>(8)
        val registry = EventHandlerRegistry()
        val ok = mutableListOf<String>()
        registry.on(GroupAtMessage::class) { throw IllegalStateException("boom") }
        registry.on(GroupAtMessage::class) { ok.add(it.content) }
        val consumer = EventConsumer(channel, registry)
        consumer.start(this)
        repeat(2) {
            channel.send(Json.decodeFromString(EventPayload.serializer(), groupPayloadJson))
        }
        channel.close()
        consumer.await()
        // 两个 handler 都被调，两次事件都处理——异常被吞
        assertEquals(listOf("hi", "hi"), ok)
    }

    @Test
    fun `decode failure routes to UnknownEvent`() = runTest {
        val channel = Channel<EventPayload>(8)
        val registry = EventHandlerRegistry()
        val unknown = mutableListOf<UnknownEvent>()
        registry.on(UnknownEvent::class) { unknown.add(it) }
        val consumer = EventConsumer(channel, registry)
        consumer.start(this)
        channel.send(
            EventPayload(
                id = "bad-1", op = 0, t = "GROUP_AT_MESSAGE_CREATE",
                d = Json.parseToJsonElement("""{"author":123}""").let { it as kotlinx.serialization.json.JsonObject })
        )
        channel.close()
        consumer.await()
        assertEquals(1, unknown.size)
        assertEquals("GROUP_AT_MESSAGE_CREATE", unknown[0].type)
    }

    @Test
    fun `no subscriber does not throw`() = runTest {
        val channel = Channel<EventPayload>(8)
        val consumer = EventConsumer(channel, EventHandlerRegistry())
        consumer.start(this)
        channel.send(Json.decodeFromString(EventPayload.serializer(), groupPayloadJson))
        channel.close()
        consumer.await()
        assertTrue(true) // 到这里没崩即通过
    }
}
