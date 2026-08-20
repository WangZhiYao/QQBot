package cn.floriax.qqbot.scheduler

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * CoroutineBotScheduler 的单元测试（基于虚拟时间）：覆盖每秒 cron 任务的重复触发、
 * Quartz 表达式解析与下次执行时间计算，以及单次任务失败不影响后续调度的容错行为。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineBotSchedulerTest {

    private val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))

    @Test
    fun `every second cron fires repeatedly under virtual time`() = runTest {
        // 虚拟时钟：以虚拟 now 为基准（delay 用虚拟时间快进）
        var virtualNow = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val fired = mutableListOf<String>()
        val s = CoroutineBotScheduler(backgroundScope, parser, now = { virtualNow })
        s.schedule("* * * * * ?", "tick") {
            fired.add("x")
            virtualNow = virtualNow.plusSeconds(1) // 模拟真实时钟随触发推进
        }
        advanceTimeBy(3_500)
        runCurrent()
        s.shutdown()
        assertTrue(fired.size >= 3, "fired=${fired.size}")
    }

    @Test
    fun `cron parser accepts quartz expression and computes next`() {
        val cron = parser.parse("0 0 9 * * ?")
        val exec = ExecutionTime.forCron(cron)
        val next = exec.nextExecution(ZonedDateTime.now()).orElse(null)!!
        assertTrue(next.isAfter(ZonedDateTime.now()))
    }

    @Test
    fun `job failure does not prevent next run`() = runTest {
        var virtualNow = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        var calls = 0
        val s = CoroutineBotScheduler(backgroundScope, parser, now = { virtualNow })
        s.schedule("* * * * * ?", "flaky") {
            calls++
            virtualNow = virtualNow.plusSeconds(1)
            if (calls == 1) throw IllegalStateException("first run fails")
        }
        advanceTimeBy(2_500)
        runCurrent()
        s.shutdown()
        assertTrue(calls >= 2, "calls=$calls")
    }
}
