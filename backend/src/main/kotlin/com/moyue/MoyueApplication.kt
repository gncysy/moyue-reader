package com.moyue
 
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
 
/**
 * 应用启动监听器
 * 替代原 Spring Boot 的 ApplicationStartupListener
 */
object ApplicationStartupListener {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 应用启动时执行初始化任务
     */
    fun onApplicationReady() {
        logger.info { "应用完全启动，开始执行初始化任务..." }
        
        try {
            // 创建工作目录
            createWorkingDirectories()
            
            // 打印启动信息
            printStartupInfo()
            
            logger.info { "应用初始化完成！" }
        } catch (e: Exception) {
            logger.error(e) { "应用初始化失败" }
            throw e
        }
    }
    
    private fun createWorkingDirectories() {
        val baseDir = System.getProperty("user.dir")
        val workingDirs = listOf("logs", "cache", "sandbox", "temp")
        
        workingDirs.forEach { dirName ->
            try {
                val dir = Paths.get(baseDir, dirName)
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                    logger.info { "创建工作目录: ${dir.toAbsolutePath()}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "创建工作目录失败: $dirName" }
            }
        }
    }
    
    private fun printStartupInfo() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        logger.info {
            """
            |========================================
            |墨阅后端启动信息
            |应用名称: 墨阅后端
            |应用版本: 1.0.0
            |启动时间: $timestamp
            |Java 版本: ${System.getProperty("java.version")}
            |操作系统: ${System.getProperty("os.name")}
            |工作目录: ${System.getProperty("user.dir")}
            |========================================
            """.trimMargin()
        }
    }
}
 
/**
 * 应用属性配置
 */
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
