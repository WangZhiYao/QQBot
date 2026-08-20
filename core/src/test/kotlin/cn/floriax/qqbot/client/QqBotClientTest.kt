package cn.floriax.qqbot.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 基于 MockEngine 的 QqBotClient 单元测试：验证群聊/C2C 消息回复的请求端点与请求体、
 * 错误响应到 QqApiException 的映射、HTTP 401 触发 token 刷新并重试一次，以及 msg_seq 生成器的自增语义。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqBotClientTest {

    /** 按 URL 分流：token 端点与业务端点各自独立响应队列。 */
    private class Recorder {
        val requests = mutableListOf<Pair<String, String>>() // url, body
        val tokenResponses = ArrayDeque<String>()
        val apiResponses = ArrayDeque<Pair<HttpStatusCode, String>>()

        fun tokenOk(token: String = "tok") {
            tokenResponses.add("""{"access_token":"$token","expires_in":7200}""")
        }
    }

    /** 构造使用 MockEngine 的 QqBotClient，请求按 Recorder 的分流规则返回预设响应。 */
    private fun client(rec: Recorder): QqBotClient {
        val engine = MockEngine { req ->
            val body = (req.body as? TextContent)?.text ?: ""
            val url = req.url.toString()
            rec.requests.add(url to body)
            if (url.contains("getAppAccessToken")) {
                respond(
                    rec.tokenResponses.removeFirst().toByteArray(), HttpStatusCode.OK,
                    headersOf("Content-Type" to listOf("application/json"))
                )
            } else {
                val (status, payload) = rec.apiResponses.removeFirst()
                respond(payload.toByteArray(), status, headersOf("Content-Type" to listOf("application/json")))
            }
        }
        return QqBotClient(appId = "app1", appSecret = "sec1", clientFactory = { HttpClient(engine) })
    }

    @Test
    fun `reply group message posts to v2 group messages endpoint`() = runTest {
        val rec = Recorder().apply {
            tokenOk()
            apiResponses.add(HttpStatusCode.OK to """{"err_code":0,"message":"ok","trace_id":"t1"}""")
        }
        val c = client(rec)
        c.replyGroupMessage(openMsgId = "om1", content = "hi", msgId = "m1")
        val (url, body) = rec.requests.last()
        assertTrue(url.endsWith("/v2/groups/om1/messages"), url)
        assertTrue(body.contains("\"msg_type\":0"))
        assertTrue(body.contains("\"content\":\"hi\""))
        assertTrue(body.contains("\"msg_id\":\"m1\""))
    }

    @Test
    fun `reply c2c posts to v2 users messages endpoint`() = runTest {
        val rec = Recorder().apply {
            tokenOk()
            apiResponses.add(HttpStatusCode.OK to """{"err_code":0,"message":"ok","trace_id":"t1"}""")
        }
        val c = client(rec)
        c.replyC2CMessage(openMsgId = "uo1", content = "yo", msgId = "m2")
        val (url, body) = rec.requests.last()
        assertTrue(url.endsWith("/v2/users/uo1/messages"), url)
        assertTrue(body.contains("\"msg_id\":\"m2\""))
    }

    @Test
    fun `error body maps to QqApiException`() = runTest {
        val rec = Recorder().apply {
            tokenOk()
            apiResponses.add(
                HttpStatusCode.OK to
                        """{"err_code":40034005,"message":"回复消息msg_id已过期","trace_id":"tr1"}"""
            )
        }
        val c = client(rec)
        val ex = assertFailsWith<QqApiException> { c.replyGroupMessage("om1", "hi", "m1") }
        assertEquals(40034005, ex.rawCode)
        assertTrue(ex.errorCode.isMsgExpired)
        assertEquals("tr1", ex.traceId)
    }

    @Test
    fun `http 401 triggers token refresh then retry once`() = runTest {
        val rec = Recorder().apply {
            tokenOk("tok-1")
            tokenOk("tok-2") // 强刷后的新 token
            apiResponses.add(HttpStatusCode.Unauthorized to """{"err_code":11243,"message":"token错误","trace_id":"t"}""")
            apiResponses.add(HttpStatusCode.OK to """{"err_code":0,"message":"ok","trace_id":"t2"}""")
        }
        val c = client(rec)
        c.replyGroupMessage("om1", "hi", "m1") // 不抛异常：重试成功
        assertEquals(2, rec.requests.filter { it.first.contains("/v2/") }.size)
        assertEquals(2, rec.requests.filter { it.first.contains("getAppAccessToken") }.size)
    }

    @Test
    fun `msg seq generator increments`() {
        val g = MsgSeqGenerator()
        assertEquals(1u, g.next("k1"))
        assertEquals(2u, g.next("k1"))
        assertEquals(1u, g.next("k2"))
    }
}
