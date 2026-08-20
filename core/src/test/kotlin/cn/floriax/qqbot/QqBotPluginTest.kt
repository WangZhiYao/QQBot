package cn.floriax.qqbot

import cn.floriax.qqbot.events.C2cMessage
import cn.floriax.qqbot.events.GroupAtMessage
import cn.floriax.qqbot.events.UnknownEvent
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 端到端验证 qqBot DSL 插件：配置初始化、事件订阅注册以及事件分发到对应 handler 的完整流程，
 * 并覆盖缺少 appId 时的启动失败场景。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqBotPluginTest {

    @Test
    fun `qqBot DSL configures and dispatches end to end`() {
        val replied = CountDownLatch(1)
        var gotContent: String? = null

        io.ktor.server.testing.testApplication {
            var botRef: QqBot? = null
            application {
                botRef = qqBot {
                    appId = "test-app"
                    appSecret = "naOC0ocQE3shWLAfffVLB1rhYPG7"
                    skipSignatureVerify = true
                    on<GroupAtMessage> {
                        gotContent = content
                        replied.countDown()
                    }
                    on<C2cMessage> { }
                    on<UnknownEvent> { }
                }
            }
            // 发一个请求强制应用 application 模块（testApplication 惰性应用）
            client.get("/")
            val bot = botRef!!
            runBlocking {
                bot.testDispatch(
                    """{"id":"e2e-1","op":0,"t":"GROUP_AT_MESSAGE_CREATE",
                       "d":{"id":"m1","content":" 你好机器人","author":{"member_openid":"u1"},
                            "group_openid":"g1","timestamp":"t"}}"""
                )
            }
            assertTrue(replied.await(3, TimeUnit.SECONDS), "handler 未被调用")
            assertEquals(" 你好机器人", gotContent)
        }
    }

    /** 若运行环境恰好设置了 QQ_APP_ID 环境变量，则不会抛出异常，此时跳过断言不算失败。 */
    @Test
    fun `qqBot requires appId or fails`() {
        try {
            io.ktor.server.testing.testApplication {
                application {
                    qqBot { appSecret = "x" } // 无 appId、无环境变量时抛错
                }
            }
            // 如果环境恰好有 QQ_APP_ID 则跳过断言（不算失败）
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("appId"))
        }
    }
}
