package cn.floriax.qqbot.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

/**
 * HttpClient 构建工厂：默认使用 CIO 引擎并安装 ContentNegotiation；
 * 通过工厂函数注入，便于测试中替换为 MockEngine。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
object HttpClientFactory {

    /** 提供默认的 HttpClient 构建 lambda（CIO 引擎 + ContentNegotiation）。 */
    fun default(): () -> HttpClient = {
        HttpClient(CIO) {
            install(ContentNegotiation)
        }
    }
}
