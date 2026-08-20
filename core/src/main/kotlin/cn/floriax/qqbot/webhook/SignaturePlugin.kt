package cn.floriax.qqbot.webhook

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

/**
 * 验签插件配置：AppSecret、跳过开关、时间戳容忍窗口与拦截的路径前缀。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
/** 验签时间戳容忍窗口默认值：±5 分钟。 */
private const val DEFAULT_TOLERANCE_MILLIS = 5 * 60 * 1000L

class SignatureConfig {
    internal var appSecret: String = ""
    internal var skip: Boolean = false
    internal var toleranceMillis: Long = DEFAULT_TOLERANCE_MILLIS

    /** 测试可注入的时钟（毫秒）。 */
    internal var clock: () -> Long = System::currentTimeMillis

    /** 验签拦截的路径；应与 webhook 路由路径一致。 */
    internal var pathPrefix: String = "/qq/webhook"
}

/**
 * QQ 回调验签插件：
 * 1. 缓存原始 body（验签对象 = X-Signature-Timestamp + rawBody）
 * 2. 验证 Ed25519 签名（AppSecret 重复扩展为 seed 派生公钥）
 * 3. 时间戳新鲜度检查（防重放，默认 ±5 分钟）
 * 失败回 401 并终止管线。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
val SignaturePlugin = createApplicationPlugin("SignaturePlugin", ::SignatureConfig) {
    val cfg = pluginConfig
    onCall { call ->
        val path = call.request.path()
        val isPost = call.request.httpMethod.value.equals("POST", ignoreCase = true)
        if (!path.startsWith(cfg.pathPrefix) || !isPost) return@onCall
        if (cfg.skip || cfg.appSecret.isEmpty()) return@onCall

        val rawBody = call.receiveChannel().readRemaining().readByteArray()
        call.attributes.put(RawBodyKey, rawBody)

        if (!verify(call, rawBody, cfg)) {
            call.respondText("invalid signature", status = HttpStatusCode.Unauthorized)
        }
    }
}

/** 秒 → 毫秒换算系数。 */
private const val MILLIS_PER_SECOND = 1000L

/** 十六进制基数。 */
private const val HEX_RADIX = 16

/** 用于在 call attributes 中缓存原始请求体的 key。 */
internal val RawBodyKey = AttributeKey<ByteArray>("qqbot-raw-body")

/** 取出验签插件缓存的原始请求体（供后续路由解析 JSON 复用，避免二次读取）。 */
internal fun ApplicationCall.rawBody(): ByteArray = attributes[RawBodyKey]

/** 校验请求头中的 Ed25519 签名与时间戳新鲜度，任一环节缺失或非法均返回 false。 */
private fun verify(call: ApplicationCall, rawBody: ByteArray, cfg: SignatureConfig): Boolean {
    val sigHex = call.request.headers["X-Signature-Ed25519"] ?: return false
    val ts = call.request.headers["X-Signature-Timestamp"] ?: return false
    val tsMillis = ts.toLongOrNull() ?: return false
    // 平台时间戳为秒级
    if (kotlin.math.abs(cfg.clock() - tsMillis * MILLIS_PER_SECOND) > cfg.toleranceMillis) return false
    val sig = runCatching { sigHex.chunked(2).map { it.toInt(HEX_RADIX).toByte() }.toByteArray() }
        .getOrNull() ?: return false
    val message = (ts + String(rawBody, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
    return Ed25519.verify(message, sig, Ed25519.publicKeyFrom(cfg.appSecret))
}
