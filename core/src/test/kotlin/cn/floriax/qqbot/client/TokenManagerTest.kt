package cn.floriax.qqbot.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * TokenManager 的单元测试：覆盖 token 的获取与缓存复用、
 * 剩余有效期低于刷新阈值时主动刷新，以及获取失败时抛出 TokenFetchException 的场景。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class TokenManagerTest {

    @Test
    fun `fetches and caches token`() = runTest {
        var calls = 0
        val mgr = TokenManager(
            appId = "app1", appSecret = "sec1",
            clientFactory = {
                HttpClient(MockEngine { _: HttpRequestData ->
                    calls++
                    respond(
                        """{"access_token":"tok-A","expires_in":7200}""",
                        HttpStatusCode.OK, headersOf("Content-Type" to listOf("application/json"))
                    )
                })
            },
            clock = { 1_000L },
        )
        assertEquals("tok-A", mgr.token())
        assertEquals("tok-A", mgr.token())
        assertEquals(1, calls)
    }

    @Test
    fun `refreshes when remaining life below threshold`() = runTest {
        var now = 1_000L
        var calls = 0
        val mgr = TokenManager(
            appId = "app1", appSecret = "sec1",
            clientFactory = {
                HttpClient(MockEngine { _: HttpRequestData ->
                    calls++
                    respond(
                        """{"access_token":"tok-$calls","expires_in":7200}""",
                        HttpStatusCode.OK, headersOf("Content-Type" to listOf("application/json"))
                    )
                })
            },
            clock = { now },
        )
        assertEquals("tok-1", mgr.token())
        now = 1_000 + (7200 - 119) * 1000L  // 剩余 119s < 120s 阈值
        assertEquals("tok-2", mgr.token())
        assertEquals(2, calls)
    }

    @Test
    fun `http failure throws TokenFetchException`() = runTest {
        val mgr = TokenManager(
            appId = "app1", appSecret = "sec1",
            clientFactory = {
                HttpClient(MockEngine { _: HttpRequestData ->
                    respond(
                        "bad", HttpStatusCode.InternalServerError,
                        headersOf("Content-Type" to listOf("application/json"))
                    )
                })
            },
            clock = { 0L },
        )
        assertFailsWith<TokenFetchException> { mgr.token() }
    }
}
