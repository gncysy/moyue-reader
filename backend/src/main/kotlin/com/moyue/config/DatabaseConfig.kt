package com.moyue.config
 
import com.moyue.model.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
 
/**
 * 数据库配置
 * 使用 Exposed ORM + HikariCP 连接池
 */
fun Application.configureDatabase() {
    val logger = KotlinLogging.logger {}
    
    // 获取数据库配置
    val dbUrl = environment.config.property("ktor.database.url").getString()
    val dbUser = environment.config.property("ktor.database.user").getString()
    val dbPassword = environment.config.property("ktor.database.password").getString()
    
    // 配置 HikariCP 连接池
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.h2.Driver" // 或 "org.sqlite.JDBC"
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30000
        poolName = "MoyueHikariPool"
    }
    
    val dataSource = HikariDataSource(hikariConfig)
    
    // 连接数据库
    Database.connect(dataSource)
    
    logger.info { "数据库连接成功: $dbUrl" }
    
    // 自动创建表
    transaction {
        SchemaUtils.create(
            Books,
            BookChapters,
            BookSources,
            BookSourceRules
        )
    }
    
    logger.info { "数据库表创建完成" }
}
