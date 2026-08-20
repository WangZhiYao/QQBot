package cn.floriax.qqbot.scheduler

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZonedDateTime

/**
 * 调度抽象：cron 表达式（Quartz 风格）驱动挂起任务。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
interface BotScheduler {
    /** 按 cron 表达式注册周期性挂起任务，name 用于日志标识。 */
    fun schedule(cron: String, name: String, action: suspend () -> Unit)

    /** 取消所有已注册任务并释放资源。 */
    fun shutdown()
}

/**
 * 协程实现：每个任务一个长驻协程，计算下次执行时间后 delay。
 * now 可注入（测试虚拟时钟）。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class CoroutineBotScheduler(
    scope: CoroutineScope,
    private val parser: CronParser = CronParser(
        CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
    ),
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : BotScheduler {
    private val logger = LoggerFactory.getLogger("BotScheduler")
    private val scope = CoroutineScope(scope.coroutineContext + SupervisorJob())
    private val jobs = mutableListOf<Job>()

    override fun schedule(cron: String, name: String, action: suspend () -> Unit) {
        val execution = ExecutionTime.forCron(parser.parse(cron))
        jobs += scope.launch {
            while (true) {
                val current = now()
                val next = execution.nextExecution(current).orElse(null) ?: break
                val wait = Duration.between(current, next).toMillis()
                if (wait > 0) delay(wait)
                runCatching { action() }
                    .onFailure { logger.error("scheduled job '{}' failed", name, it) }
            }
        }
    }

    override fun shutdown() {
        jobs.forEach { it.cancel() }
    }
}
