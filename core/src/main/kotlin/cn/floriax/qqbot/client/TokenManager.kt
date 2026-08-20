package cn.floriax.qqbot.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** 秒 → 毫秒换算系数。 */
private const val MILLIS_PER_SECOND = 1000L

/**
 * access token 管理器：负责从 QQ 开放平台获取并缓存 getAppAccessToken，
 * 在过期前（提前 [refreshThresholdMillis]）自动刷新，并通过 Mutex 保证并发下只刷新一次。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal class TokenManager(
    private val appId: String,
    private val appSecret: String,
    clientFactory: () -> HttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshThresholdMillis: Long = 120_000,
) {
    private val client by lazy { clientFactory() }
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expireAt: Long = 0L

    /** 获取有效 token：缓存未到期直接返回，否则加锁刷新（双重检查避免并发重复刷新）。 */
    suspend fun token(): String {
        cachedToken?.let { t ->
            if (clock() < expireAt - refreshThresholdMillis) return t
        }
        return mutex.withLock {
            cachedToken?.let { t ->
                if (clock() < expireAt - refreshThresholdMillis) return@withLock t
            }
            refresh()
        }
    }

    /** 强制刷新 token（忽略缓存），用于 401 后重试。 */
    suspend fun forceRefresh(): String = mutex.withLock { refresh() }

    /** 请求 token 接口并更新缓存与过期时间，失败抛 [TokenFetchException]。 */
    private suspend fun refresh(): String {
        val response: HttpResponse = try {
            client.post("https://api.bot.qq.com/app/getAppAccessToken") {
                contentType(ContentType.Application.Json)
                setBody("""{"appId":"$appId","clientSecret":"$appSecret"}""")
            }
        } catch (e: Exception) {
            throw TokenFetchException("token request failed", e)
        }
        if (response.status != HttpStatusCode.OK) {
            throw TokenFetchException("token endpoint returned ${response.status.value}")
        }
        val body = json.decodeFromString(TokenResponse.serializer(), response.body<String>())
        cachedToken = body.accessToken
        expireAt = clock() + body.expiresIn * MILLIS_PER_SECOND
        return body.accessToken
    }
}
