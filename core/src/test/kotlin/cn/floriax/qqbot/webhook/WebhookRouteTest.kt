package cn.floriax.qqbot.webhook

import cn.floriax.qqbot.bus.EventDeduplicator
import cn.floriax.qqbot.bus.EventPayload
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 测试用 Ed25519 签名密钥（官方文档示例 AppSecret）。 */
private const val APP_SECRET = "naOC0ocQE3shWLAfffVLB1rhYPG7"

/** 测试用固定签名时间戳，与 Harness 中固定的 clock 对齐。 */
private const val FIXED_TS = "1700000000"

/** 测试环境装配：事件队列与固定时钟的去重器。 */
private class Harness(channelCapacity: Int = 1024) {
    val channel = Channel<EventPayload>(channelCapacity)
    val dedup = EventDeduplicator(4096, 600_000, clock = { 1_700_000_000_000L })
}

/** 在测试应用上安装签名校验插件并挂载 webhook 路由。 */
private fun Application.installWebhook(h: Harness) {
    install(SignaturePlugin) {
        appSecret = APP_SECRET
        clock = { 1_700_000_000_000L }
    }
    routing {
        webhookRoute(h.channel, h.dedup, APP_SECRET)
    }
}

/** 按官方算法用固定时间戳对 body 签名，并以带签名头的方式 POST 到 /qq/webhook。 */
private suspend fun ApplicationTestBuilder.postSigned(body: String) =
    client.post("/qq/webhook") {
        setBody(body)
        val sig = Ed25519.sign(FIXED_TS + body, Ed25519.seedFromSecret(APP_SECRET))
        header("X-Signature-Ed25519", Ed25519.toHex(sig))
        header("X-Signature-Timestamp", FIXED_TS)
    }

/**
 * Webhook 路由集成测试：覆盖 op0 事件首次投递入队与重复去重、
 * op13 回调验证签名响应、畸形 JSON 容错以及队列满时返回 503。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class WebhookRouteTest {

    /** op0 事件分发场景的 payload 固件。 */
    private fun dispatchPayload(): String =
        """{"id":"evt-1","op":0,"t":"GROUP_AT_MESSAGE_CREATE","d":{"id":"m1","content":"hi"}}"""

    /** op13 回调验证场景的 payload 固件。 */
    private fun validationPayload(): String =
        """{"op":13,"d":{"plain_token":"Arq0D5A61EgUu4OxUvOp","event_ts":"1725442341"}}"""

    @Test
    fun `op0 first delivery returns 200 and enqueues`() = testApplication {
        val h = Harness()
        application { installWebhook(h) }
        val resp = postSigned(dispatchPayload())
        assertEquals(HttpStatusCode.OK, resp.status)
        val received = h.channel.tryReceive().getOrNull()
        assertEquals("evt-1", received?.id)
        assertEquals("GROUP_AT_MESSAGE_CREATE", received?.t)
    }

    @Test
    fun `op0 duplicate id returns 200 without enqueue`() = testApplication {
        val h = Harness()
        application { installWebhook(h) }
        postSigned(dispatchPayload())
        val resp2 = postSigned(dispatchPayload())
        assertEquals(HttpStatusCode.OK, resp2.status)
        assertEquals("evt-1", h.channel.tryReceive().getOrNull()?.id)
        assertNull(h.channel.tryReceive().getOrNull()) // 第二条没入队
    }

    @Test
    fun `op13 validation returns signed payload`() = testApplication {
        val h = Harness()
        application { installWebhook(h) }
        val resp = postSigned(validationPayload())
        assertEquals(HttpStatusCode.OK, resp.status)
        val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertEquals("Arq0D5A61EgUu4OxUvOp", json["plain_token"]?.jsonPrimitive?.content)
        val sig = json["signature"]?.jsonPrimitive?.content ?: ""
        // Ed25519 签名 64 字节 → hex 128 字符；且用官方算法可验回
        assertEquals(128, sig.length)
        val verified = Ed25519.verify(
            "1725442341Arq0D5A61EgUu4OxUvOp".toByteArray(),
            sig.chunked(2).map { it.toInt(16).toByte() }.toByteArray(),
            Ed25519.publicKeyFrom(APP_SECRET),
        )
        assertTrue(verified)
    }

    @Test
    fun `malformed json returns 200`() = testApplication {
        val h = Harness()
        application { installWebhook(h) }
        val resp = postSigned("not-json")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertNull(h.channel.tryReceive().getOrNull())
    }

    @Test
    fun `full queue returns 503`() = testApplication {
        val h = Harness(channelCapacity = 1)
        application { installWebhook(h) }
        h.channel.trySend(EventPayload(id = "pre", op = 0)) // 预填充至满
        val resp = postSigned(dispatchPayload())
        assertEquals(HttpStatusCode.ServiceUnavailable, resp.status)
        assertEquals("pre", h.channel.tryReceive().getOrNull()?.id)
        assertNull(h.channel.tryReceive().getOrNull())
    }
}
