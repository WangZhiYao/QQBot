package cn.floriax.qqbot.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * QqErrorCode 枚举的单元测试：覆盖 fromCode 的已知码解析与未知码回退、
 * 各分类属性（可重试/权限拒绝/限频/消息过期）的判定、错误码唯一性以及官方分段码的完整性抽样。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QqErrorCodeTest {

    @Test
    fun `fromCode resolves known code`() {
        assertEquals(QqErrorCode.CANNOT_SEND_EMPTY_MESSAGE, QqErrorCode.fromCode(50006))
    }

    @Test
    fun `fromCode falls back to UNKNOWN`() {
        assertEquals(QqErrorCode.UNKNOWN, QqErrorCode.fromCode(999999999))
        assertEquals(-1, QqErrorCode.UNKNOWN.code)
    }

    @Test
    fun `category properties`() {
        assertTrue(QqErrorCode.fromCode(11281).isRetryable)      // 检查管理员失败-系统错误
        assertTrue(QqErrorCode.fromCode(11253).isPermissionDenied)
        assertTrue(QqErrorCode.UNKNOWN.isRateLimited.not())      // 未知码不误判
        assertTrue(QqErrorCode.fromCode(1100100).isRateLimited)  // 安全打击限频
        assertTrue(QqErrorCode.fromCode(304027).isMsgExpired)
    }

    @Test
    fun `codes are unique`() {
        val codes = QqErrorCode.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `fromCode never returns null for defined entries`() {
        // 抽样官方各分段首码
        listOf(
            10001, 301000, 302000, 304003, 306001, 501001, 502001, 503001, 504001,
            610001, 620001, 630001, 1100100, 3300006
        ).forEach { c ->
            assertTrue(QqErrorCode.entries.any { it.code == c }, "missing $c")
        }
    }
}
