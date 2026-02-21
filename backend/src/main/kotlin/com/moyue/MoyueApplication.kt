package com.moyue
 
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
 
@SpringBootApplication(
    scanBasePackages = ["com.moyue"]
)
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AppProperties::class)
class MoyueApplication
 
/**
 * 应用配置属性
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    var name: String = "墨阅后端",
    var version: String = "1.0.0",
    var portFile: String = "backend-port.txt",
    var portFileDirectory: String? = null,
    var logStartupInfo: Boolean = true,
    var createWorkingDirs: Boolean = true,
    var workingDirs: List<String> = listOf(
        "logs",
        "cache",
        "sandbox",
        "temp"
    )
)
 
/**
 * 主函数
 */
fun main(args: Array<String>) {
    try {
        val context = runApplication<MoyueApplication>(*args)
        
        // 获取环境变量
        val env = context.environment
        val logger = LoggerFactory.getLogger("Main")
        
        logger.info("========================================")
        logger.info("墨阅后端启动成功！")
        logger.info("版本: {}", env.getProperty("app.version", "1.0.0"))
        logger.info("环境: {}", env.activeProfiles.joinToString(", ") { if (it.isEmpty()) "default" else it })
        logger.info("========================================")
        
    } catch (e: Exception) {
        val logger = LoggerFactory.getLogger("Main")
        logger.error("应用启动失败！", e)
        System.exit(1)
    }
}
 
/**
 * 应用启动事件监听器 - 在应用完全启动后执行
 */
@Component
class ApplicationStartupListener(
    private val appProperties: AppProperties,
    private val env: Environment
) : ApplicationListener<ApplicationReadyEvent> {
    
    private val logger = LoggerFactory.getLogger(ApplicationStartupListener::class.java)
    
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        logger.info("应用完全启动，开始执行初始化任务...")
        
        try {
            // 创建工作目录
            if (appProperties.createWorkingDirs) {
                createWorkingDirectories()
            }
            
            // 检查必要配置
            validateConfiguration()
            
            // 打印启动信息
            if (appProperties.logStartupInfo) {
                printStartupInfo()
            }
            
            logger.info("应用初始化完成！")
        } catch (e: Exception) {
            logger.error("应用初始化失败", e)
            throw e
        }
    }
    
    private fun createWorkingDirectories() {
        val baseDir = System.getProperty("user.dir")
        
        appProperties.workingDirs.forEach { dirName ->
            try {
                val dir = Paths.get(baseDir, dirName)
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                    logger.info("创建工作目录: {}", dir.toAbsolutePath())
                }
            } catch (e: Exception) {
                logger.error("创建工作目录失败: {}", dirName, e)
            }
        }
    }
    
    private fun validateConfiguration() {
        // 检查数据库配置
        val dbUrl = env.getProperty("spring.datasource.url")
        if (dbUrl.isNullOrBlank()) {
            logger.warn("数据库配置缺失，应用可能无法正常工作")
        }
        
        // 检查缓存配置
        val cacheConfig = env.getProperty("spring.cache.type")
        if (cacheConfig.isNullOrBlank()) {
            logger.info("未配置缓存，将使用默认内存缓存")
        }
    }
    
    private fun printStartupInfo() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        logger.info("========================================")
        logger.info("墨阅后端启动信息")
        logger.info("应用名称: {}", appProperties.name)
        logger.info("应用版本: {}", appProperties.version)
        logger.info("启动时间: {}", timestamp)
        logger.info("Java 版本: {}", System.getProperty("java.version"))
        logger.info("操作系统: {}", System.getProperty("os.name"))
        logger.info("工作目录: {}", System.getProperty("user.dir"))
        logger.info("========================================")
    }
}
 
/**
 * 端口日志记录器 - 记录实际端口号
 */
@Component
class PortLogger(
    private val appProperties: AppProperties
) : ApplicationListener<WebServerInitializedEvent> {
    
    private val logger = LoggerFactory.getLogger(PortLogger::class.java)
    
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        
        logger.info("========================================")
        logger.info("🚀 Web服务器启动成功！")
        logger.info("📌 实际端口: {}", port)
        logger.info("🌐 健康检查: http://localhost:{}/api/health", port)
        logger.info("📚 API文档: http://localhost:{}/swagger-ui.html", port)
        logger.info("========================================")
        
        // 将端口号写入文件
        writePortToFile(port)
    }
    
    private fun writePortToFile(port: Int) {
        try {
            val portFileDirectory = appProperties.portFileDirectory 
                ?: System.getProperty("user.dir")
            
            val portFile = Paths.get(portFileDirectory, appProperties.portFile)
            
            // 确保目录存在
            Files.createDirectories(portFile.parent)
            
            // 写入端口号
            Files.write(portFile, port.toString().toByteArray())
            
            logger.info("端口号已写入文件: {}", portFile.toAbsolutePath())
            
        } catch (e: Exception) {
            logger.warn("写入端口号文件失败: {}", e.message)
        }
    }
}
 
/**
 * 应用启动前监听器 - 在应用启动前执行
 */
@Component
class ApplicationPreStartListener : ApplicationListener<ApplicationStartedEvent> {
    
    private val logger = LoggerFactory.getLogger(ApplicationPreStartListener::class.java)
    
    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        logger.info("应用开始启动，正在初始化...")
        
        // 检查 Java 版本
        val javaVersion = System.getProperty("java.version")
        val majorVersion = javaVersion.split(".")[0].toIntOrNull() ?: 8
        
        if (majorVersion < 17) {
            logger.warn("推荐使用 Java 17 或更高版本，当前版本: {}", javaVersion)
        }
        
        // 打印系统信息
        logger.info("Java 版本: {}", javaVersion)
        logger.info("JVM 名称: {}", System.getProperty("java.vm.name"))
        logger.info("操作系统: {} {}", System.getProperty("os.name"), System.getProperty("os.version"))
        logger.info("处理器数量: {}", Runtime.getRuntime().availableProcessors())
        logger.info("最大内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024)
    }
}
 
/**
 * 优雅关闭配置
 */
@Component
class GracefulShutdownBean {
    
    private val logger = LoggerFactory.getLogger(GracefulShutdownBean::class.java)
    
    init {
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("正在关闭应用...")
            logger.info("应用已安全关闭")
        })
    }
}
