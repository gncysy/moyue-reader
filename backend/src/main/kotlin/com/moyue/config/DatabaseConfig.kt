package com.moyue.config
 
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.annotation.PersistenceExceptionTranslation
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
 
/**
 * 数据库配置
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 使用 HikariCP 连接池
 * 使用 SQLite 数据库（开发环境）
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@Configuration
@EnableJpaRepositories(basePackages = ["com.moyue.repository"])
@EnableJpaAuditing
@EnableTransactionManagement
class DatabaseConfig {
    
    @Value("\${spring.datasource.url:jdbc:sqlite:moyue.db}")
    private lateinit var jdbcUrl: String
    
    @Value("\${spring.datasource.driver-class-name:org.sqlite.JDBC}")
    private lateinit var driverClassName: String
    
    @Value("\${spring.datasource.username:}")
    private var username: String = ""
    
    @Value("\${spring.datasource.password:}")
    private var password: String = ""
    
    @Value("\${spring.datasource.hikari.maximum-pool-size:10}")
    private var maxPoolSize: Int = 10
    
    @Value("\${spring.datasource.hikari.minimum-idle:5}")
    private var minIdle: Int = 5
    
    @Value("\${spring.datasource.hikari.connection-timeout:30000}")
    private var connectionTimeout: Long = 30000
    
    @Value("\${spring.datasource.hikari.idle-timeout:600000}")
    private var idleTimeout: Long = 600000
    
    @Value("\${spring.datasource.hikari.max-lifetime:1800000}")
    private var maxLifetime: Long = 1800000
    
    /**
     * 配置数据源
     */
    @Bean
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = this@DatabaseConfig.jdbcUrl
            driverClassName = this@DatabaseConfig.driverClassName
            username = this@DatabaseConfig.username
            password = this@DatabaseConfig.password
            
            // 连接池配置
            maximumPoolSize = this@DatabaseConfig.maxPoolSize
            minimumIdle = this@DatabaseConfig.minIdle
            connectionTimeout = this@DatabaseConfig.connectionTimeout
            idleTimeout = this@DatabaseConfig.idleTimeout
            maxLifetime = this@DatabaseConfig.maxLifetime
            
            // 性能优化
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            
            // 连接测试
            connectionTestQuery = "SELECT 1"
            
            // 连接池名称
            poolName = "MoyueHikariPool"
        }
        
        return HikariDataSource(config)
    }
    
    /**
     * 配置实体管理器工厂
     */
    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val vendorAdapter = HibernateJpaVendorAdapter().apply {
            // 配置数据库
            database = if (jdbcUrl.contains("sqlite")) {
                Database.SQLITE
            } else if (jdbcUrl.contains("mysql")) {
                Database.MYSQL
            } else if (jdbcUrl.contains("postgresql")) {
                Database.POSTGRESQL
            } else {
                Database.H2
            }
            
            // DDL 配置
            isGenerateDdl = true
            setShowSql = false
        }
        
        return LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            this.jpaVendorAdapter = vendorAdapter
            setPackagesToScan("com.moyue.model")
            
            // Hibernate 配置
            setJpaProperties(mapOf(
                // 方言
                "hibernate.dialect" to getHibernateDialect(),
                
                // DDL 配置
                "hibernate.hbm2ddl.auto" to "update",
                
                // SQL 格式化
                "hibernate.format_sql" to "false",
                "hibernate.use_sql_comments" to "false",
                
                // 二级缓存
                "hibernate.cache.use_second_level_cache" to "false",
                "hibernate.cache.use_query_cache" to "false",
                
                // 统计信息
                "hibernate.generate_statistics" to "false",
                
                // 批处理
                "hibernate.jdbc.batch_size" to "50",
                "hibernate.order_inserts" to "true",
                "hibernate.order_updates" to "true",
                
                // 连接释放
                "hibernate.connection.handling_mode" to "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT"
            ))
        }
    }
    
    /**
     * 配置事务管理器
     */
    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory).apply {
            // 配置事务
            defaultTimeout = 30
            isRollbackOnCommitFailure = true
            isNestedTransactionAllowed = false
        }
    }
    
    /**
     * 配置 JdbcTemplate
     */
    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource).apply {
            // 查询超时
            queryTimeout = 30
            
            // 获取最大行数
            maxRows = 10000
            
            // 获取大小
            fetchSize = 1000
        }
    }
    
    /**
     * 获取 Hibernate 方言
     */
    private fun getHibernateDialect(): String {
        return when {
            jdbcUrl.contains("sqlite") -> "org.hibernate.community.dialect.SQLiteDialect"
            jdbcUrl.contains("mysql") -> "org.hibernate.dialect.MySQLDialect"
            jdbcUrl.contains("postgresql") -> "org.hibernate.dialect.PostgreSQLDialect"
            jdbcUrl.contains("h2") -> "org.hibernate.dialect.H2Dialect"
            else -> "org.hibernate.dialect.H2Dialect"
        }
    }
}
