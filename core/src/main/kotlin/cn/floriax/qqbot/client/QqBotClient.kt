package cn.floriax.qqbot.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 消息类型：文本。 */
private const val MSG_TYPE_TEXT = 0

/** 成功 HTTP 状态码下界（含）。 */
private const val HTTP_OK_MIN = 200

/** 成功 HTTP 状态码上界（含）。 */
private const val HTTP_OK_MAX = 299

/**
 * QQ OpenAPI 封装：被动回复与主动消息。
 * 错误处理：err_code != 0 或非 2xx → QqApiException；401 → token 强刷后重试一次。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqBotClient(
    appId: String,
    appSecret: String,
    clientFactory: () -> HttpClient,
    private val config: QqBotClientConfig = QqBotClientConfig(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http: HttpClient = clientFactory()
    private val tokenManager = TokenManager(appId, appSecret, clientFactory)

    /** 被动回复群消息：通过事件中的 openMsgId 与 msgId 进行回复。 */
    suspend fun replyGroupMessage(openMsgId: String, content: String, msgId: String) {
        requestWithAuthRetry("/v2/groups/$openMsgId/messages", buildJsonObject {
            put("msg_type", MSG_TYPE_TEXT)
            put("content", content)
            put("msg_id", msgId)
        })
    }

    /** 被动回复 C2C（单聊）消息：通过事件中的 openMsgId 与 msgId 进行回复。 */
    suspend fun replyC2CMessage(openMsgId: String, content: String, msgId: String) {
        requestWithAuthRetry("/v2/users/$openMsgId/messages", buildJsonObject {
            put("msg_type", MSG_TYPE_TEXT)
            put("content", content)
            put("msg_id", msgId)
        })
    }

    /** 主动发送群消息：需携带递增的 msg_seq。 */
    suspend fun sendGroupMessage(groupOpenId: String, content: String, msgSeq: UInt) {
        requestWithAuthRetry("/v2/groups/$groupOpenId/messages", buildJsonObject {
            put("msg_type", MSG_TYPE_TEXT)
            put("content", content)
            put("msg_seq", msgSeq.toInt())
        })
    }

    /** 主动发送 C2C（单聊）消息：需携带递增的 msg_seq。 */
    suspend fun sendC2C(openId: String, content: String, msgSeq: UInt) {
        requestWithAuthRetry("/v2/users/$openId/messages", buildJsonObject {
            put("msg_type", MSG_TYPE_TEXT)
            put("content", content)
            put("msg_seq", msgSeq.toInt())
        })
    }

    /** 带鉴权重试的 POST 请求：遇 401 时强制刷新 token 后重试一次；非 2xx 或 err_code != 0 抛 QqApiException。 */
    private suspend fun requestWithAuthRetry(path: String, body: JsonObject) {
        var refreshed = false
        while (true) {
            val token = tokenManager.token()
            val response: HttpResponse = http.post(config.apiBaseUrl + path) {
                contentType(ContentType.Application.Json)
                header("Authorization", "QQBot $token")
                setBody(body.toString())
            }
            if (response.status == HttpStatusCode.Unauthorized && !refreshed) {
                refreshed = true
                tokenManager.forceRefresh()
                continue
            }
            val text = response.body<String>()
            val envelope = runCatching { json.decodeFromString(ApiEnvelope.serializer(), text) }
                .getOrDefault(ApiEnvelope())
            if (response.status.value !in HTTP_OK_MIN..HTTP_OK_MAX || envelope.errCode != 0) {
                throw QqApiException(
                    errorCode = QqErrorCode.fromCode(envelope.errCode),
                    rawCode = envelope.errCode,
                    apiMessage = envelope.message,
                    traceId = envelope.traceId,
                    httpStatus = response.status.value,
                )
            }
            return
        }
    }
}
