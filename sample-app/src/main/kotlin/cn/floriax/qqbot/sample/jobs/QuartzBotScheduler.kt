package cn.floriax.qqbot.sample.jobs

import cn.floriax.qqbot.scheduler.BotScheduler
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * BotScheduler 的 Quartz 适配（示范：使用者如何替换 core 默认协程调度）。
 * Job 内 runBlocking 桥接到挂起函数（低频任务可接受）。
 * 挂起函数以 name 为键存入全局注册表，由 [SuspendJobWrapper] 在触发时取出执行。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
class QuartzBotScheduler(
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler(),
) : BotScheduler {

    /** 将挂起动作注册到注册表，并按 cron 表达式创建 Quartz Job 与 Trigger。 */
    override fun schedule(cron: String, name: String, action: suspend () -> Unit) {
        Actions.registry[name] = action
        val job = newJob(SuspendJobWrapper::class.java)
            .withIdentity("job-$name")
            .usingJobData("name", name)
            .build()
        val trigger = newTrigger()
            .withIdentity("trigger-$name")
            .withSchedule(cronSchedule(cron))
            .build()
        scheduler.scheduleJob(job, trigger)
    }

    /** 停止调度器（等待已触发任务完成）并清空动作注册表。 */
    override fun shutdown() {
        scheduler.shutdown(true)
        Actions.registry.clear()
    }

    /** 启动 Quartz 调度器，需在所有 schedule 调用之后调用。 */
    fun start() {
        scheduler.start()
    }

    /** Quartz Job 包装：按 JobDataMap 中的 name 查注册表，runBlocking 执行挂起动作并吞掉异常。 */
    class SuspendJobWrapper : Job {
        override fun execute(context: JobExecutionContext) {
            val name = context.mergedJobDataMap.getString("name") ?: return
            val action = Actions.registry[name] ?: return
            kotlinx.coroutines.runBlocking { runCatching { action() } }
        }
    }

    /** 挂起动作的全局注册表，供 Job 触发时按 name 查找。 */
    private object Actions {
        val registry = ConcurrentHashMap<String, suspend () -> Unit>()
    }
}
