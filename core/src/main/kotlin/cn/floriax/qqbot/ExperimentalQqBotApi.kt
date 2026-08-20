package cn.floriax.qqbot

/**
 * 标注尚不稳定的实验性 API，使用方必须显式 opt-in（@OptIn）才能调用，
 * 以避免后续签名变更对使用者造成静默破坏。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalQqBotApi
