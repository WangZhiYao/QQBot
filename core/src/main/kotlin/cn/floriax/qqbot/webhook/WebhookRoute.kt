package cn.floriax.qqbot.webhook

import cn.floriax.qqbot.bus.EventDeduplicator
import cn.floriax.qqbot.bus.EventPayload
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val json = Json { ignoreUnknownKeys = true }

/** 平台 op 码：回调地址校验。 */
private const val OP_URL_VALIDATION = 13

/** 平台 op 码：事件分发。 */
private const val OP_DISPATCH = 0

/**
 * Webhook 薄入口：op 分流 → 去重 → 入队 → 应答。
 * - op=13：URL 校验，按官方算法签名应答
 * - op=0：去重后 trySend；成功回 200（写入去重记录），队列满回 503（不写记录，平台重推）
 * - 坏 JSON：200（防平台反复重推）
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal fun Route.webhookRoute(
    channel: Channel<EventPayload>,
    deduplicator: EventDeduplicator,
    appSecret: String,
    path: String = "/qq/webhook",
) {
    post(path) {
        val raw = runCatching { call.rawBody() }.getOrNull()
            ?: return@post call.respondText("ok", status = HttpStatusCode.OK)
        val payload = runCatching {
            json.decodeFromString(EventPayload.serializer(), String(raw, Charsets.UTF_8))
        }.getOrNull()

        if (payload == null) {
            call.respondText("ok", status = HttpStatusCode.OK)
            return@post
        }
        when (payload.op) {
            OP_URL_VALIDATION -> handleValidation(call, payload, appSecret)
            OP_DISPATCH -> {
                val id = payload.id
                if (id == null || deduplicator.isDuplicate(id)) {
                    call.respondText("ok", status = HttpStatusCode.OK)
                    return@post
                }
                val offered = channel.trySend(payload).isSuccess
                if (offered) {
                    deduplicator.markSeen(id) // 仅成功入队后写入（503 重推不误判）
                    call.respondText("ok", status = HttpStatusCode.OK)
                } else {
                    call.respondText("overloaded", status = HttpStatusCode.ServiceUnavailable)
                }
            }

            else -> call.respondText("ok", status = HttpStatusCode.OK)
        }
    }
}

/** 处理 op=13 回调地址校验：用官方算法对 event_ts + plain_token 签名并回包。 */
private suspend fun handleValidation(call: ApplicationCall, payload: EventPayload, appSecret: String) {
    val d = payload.d
    val plainToken = d?.get("plain_token")?.jsonPrimitive?.content
    val eventTs = d?.get("event_ts")?.jsonPrimitive?.content
    if (plainToken == null || eventTs == null) {
        call.respondText("ok", status = HttpStatusCode.OK)
        return
    }
    val sig = Ed25519.sign(eventTs + plainToken, Ed25519.seedFromSecret(appSecret))
    val answer = buildJsonObject {
        put("plain_token", plainToken)
        put("signature", Ed25519.toHex(sig))
    }
    call.respondText(answer.toString(), ContentType.Application.Json, HttpStatusCode.OK)
}
