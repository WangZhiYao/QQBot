package cn.floriax.qqbot.sample.db

import cn.floriax.qqbot.sample.jobs.PushTargets
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.io.File

/**
 * SQLite 数据库初始化工具：确保数据目录存在、建立连接并执行 Exposed 的 schema 迁移。
 *
 * @author WangZhiYao
 * @since 2026/8/19
 */
object SampleDb {
    /** 初始化数据库：创建父目录、连接 SQLite 并按表定义补齐缺失的 schema。 */
    fun init(path: String = "data/sample.db") {
        File(path).parentFile?.mkdirs()
        Database.connect("jdbc:sqlite:$path", driver = "org.sqlite.JDBC")
        transaction { MigrationUtils.statementsRequiredForDatabaseMigration(PushTargets, withLogs = true) }
    }
}
