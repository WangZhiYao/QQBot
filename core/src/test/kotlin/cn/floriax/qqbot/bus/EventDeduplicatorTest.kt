package cn.floriax.qqbot.bus

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EventDeduplicator 去重器测试：覆盖首次出现不判重、标记后判重、
 * 只查不写不产生副作用、TTL 过期失效以及容量满时淘汰最旧条目。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class EventDeduplicatorTest {

    @Test
    fun `unseen id is not duplicate and becomes duplicate after markSeen`() {
        val d = EventDeduplicator(capacity = 4, ttlMillis = 60_000, clock = { 0L })
        assertFalse(d.isDuplicate("a"))
        d.markSeen("a")
        assertTrue(d.isDuplicate("a"))
    }

    @Test
    fun `id not marked seen is never duplicate`() {
        val d = EventDeduplicator(capacity = 4, ttlMillis = 60_000, clock = { 0L })
        assertFalse(d.isDuplicate("a"))
        assertFalse(d.isDuplicate("a")) // 只查不写
    }

    @Test
    fun `entries expire after ttl`() {
        var now = 0L
        val d = EventDeduplicator(capacity = 4, ttlMillis = 1_000, clock = { now })
        d.markSeen("a")
        now = 1_001
        assertFalse(d.isDuplicate("a"))
    }

    @Test
    fun `capacity eviction removes oldest`() {
        val d = EventDeduplicator(capacity = 2, ttlMillis = 60_000, clock = { 0L })
        d.markSeen("a")
        d.markSeen("b")
        d.markSeen("c") // 挤掉 a
        assertFalse(d.isDuplicate("a"))
        assertTrue(d.isDuplicate("b"))
        assertTrue(d.isDuplicate("c"))
    }
}
