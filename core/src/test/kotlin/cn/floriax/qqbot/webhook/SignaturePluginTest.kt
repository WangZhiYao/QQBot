package cn.floriax.qqbot.webhook

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SignaturePlugin 签名校验插件测试：覆盖合法签名放行、签名不匹配/缺失签名头
 * 以及时间戳过期均返回 401。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class SignaturePluginTest {

    /** 测试用 Ed25519 签名密钥（官方文档示例 AppSecret）。 */
    private val appSecret = "naOC0ocQE3shWLAfffVLB1rhYPG7"

    /** 与固定 clock（1_700_000_000_000ms）对齐的签名时间戳。 */
    private val fixedTimestamp = "1700000000"

    /** 标准 op0 事件请求体固件。 */
    private fun body(): String =
        """{"op":0,"id":"e1","t":"GROUP_AT_MESSAGE_CREATE","d":{}}"""

    /** 按官方算法对 timestamp + body 签名，生成签名与时间戳两个请求头。 */
    private fun signedHeaders(body: String, timestamp: String = fixedTimestamp): Map<String, String> {
        val sig = Ed25519.sign(timestamp + body, Ed25519.seedFromSecret(appSecret))
        return mapOf(
            "X-Signature-Ed25519" to Ed25519.toHex(sig),
            "X-Signature-Timestamp" to timestamp,
        )
    }

    /** 挂一个最小路由让请求走到 handler（plugin 只拦 /qq/webhook POST）。 */
    private fun Application.installSignatureOnly() {
        install(SignaturePlugin) {
            appSecret = this@SignaturePluginTest.appSecret
            clock = { 1_700_000_000_000L }
        }
        routing {
            post("/qq/webhook") { call.respondText("ok") }
        }
    }

    @Test
    fun `valid signature passes`() = testApplication {
        application { installSignatureOnly() }
        val resp = postWebhook(body(), signedHeaders(body()))
        assertEquals(HttpStatusCode.OK, resp.status)
    }

    @Test
    fun `invalid signature returns 401`() = testApplication {
        application { installSignatureOnly() }
        val headers = signedHeaders(body()).toMutableMap().apply {
            this["X-Signature-Ed25519"] = "00" + this["X-Signature-Ed25519"]!!.drop(2)
        }
        val resp = postWebhook(body(), headers)
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `missing signature header returns 401`() = testApplication {
        application { installSignatureOnly() }
        val resp = client.post("/qq/webhook") { setBody(body()) }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `stale timestamp returns 401`() = testApplication {
        application { installSignatureOnly() }
        // clock 固定在 1_700_000_000_000ms，时间戳偏差超 5 分钟
        val stale = (1_700_000_000L - 400).toString()
        val resp = postWebhook(body(), signedHeaders(body(), stale))
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    /** 带指定请求头 POST 到 /qq/webhook。 */
    private suspend fun ApplicationTestBuilder.postWebhook(
        body: String, headers: Map<String, String>,
    ): HttpResponse =
        client.post("/qq/webhook") {
            setBody(body)
            for ((k, v) in headers) header(k, v)
        }
}
