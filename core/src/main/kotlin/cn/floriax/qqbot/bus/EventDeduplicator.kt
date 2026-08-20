package cn.floriax.qqbot.bus

/**
 * 事件 id 去重：LRU + TTL。
 * 判重与写入分离——markSeen 只在事件成功入队后调用，
 * 保证 503 重推的事件不会被误判为重复。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
internal class EventDeduplicator(
    private val capacity: Int,
    private val ttlMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val seen = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > capacity
    }

    /** 判断 id 是否在 TTL 内已出现过；过期记录顺带清除并视为未重复。 */
    @Synchronized
    fun isDuplicate(id: String): Boolean {
        val at = seen[id] ?: return false
        if (clock() - at > ttlMillis) {
            seen.remove(id)
            return false
        }
        return true
    }

    /** 记录 id 的出现时间（仅在事件成功入队后调用）。 */
    @Synchronized
    fun markSeen(id: String) {
        seen[id] = clock()
    }
}
