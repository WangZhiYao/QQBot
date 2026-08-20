package cn.floriax.qqbot.config

/**
 * 运行时配置（DSL > application.yaml > 环境变量 解析后）。
 * 汇总凭证、webhook 路径、验签与事件总线的可调参数及默认值。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
data class BotConfig(
    val appId: String,
    val appSecret: String,

    /** webhook 回调路径（默认与 sample-app 部署一致）。 */
    val webhookPath: String = "/qq/webhook",

    /** 是否跳过签名校验（仅本地调试用）。 */
    val skipSignatureVerify: Boolean = false,

    /** 事件通道容量。 */
    val channelCapacity: Int = 1024,

    /** 事件去重缓存容量。 */
    val dedupCapacity: Int = 4096,

    /** 事件去重条目存活时长（毫秒）。 */
    val dedupTtlMillis: Long = 600_000,

    /** 签名时间戳允许的时钟偏移（毫秒）。 */
    val signatureToleranceMillis: Long = 300_000,
)
