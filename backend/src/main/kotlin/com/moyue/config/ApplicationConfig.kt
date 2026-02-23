package com.moyue.config
 
import com.moyue.AppProperties
import io.ktor.server.application.*
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
 
/**
 * 应用初始化配置
 */
fun Application.initializeApplication() {
    val logger = KotlinLogging.logger {}
    
    // 创建工作目录
    createWorkingDirectories()
    
    // 加载应用属性
    loadAppProperties()
    
    logger.info { "应用初始化完成" }
}
 
private fun createWorkingDirectories() {
    val baseDir = System.getProperty("user.dir")
    val workingDirs = listOf("logs", "cache", "sandbox", "temp")
    
    workingDirs.forEach { dirName ->
        try {
            val dir = Paths.get(baseDir, dirName)
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
        } catch (e: Exception) {
            KotlinLogging.logger {}.error(e) { "创建工作目录失败: $dirName" }
        }
    }
}
 
private fun loadAppProperties(): AppProperties {
    val props = Properties()
    val inputStream = ApplicationConfig::class.java.classLoader.getResourceAsStream("application.yml")
    
    inputStream?.use {
        // 注意：YAML 需要使用专门的解析器，这里简化处理
        // 实际项目中可以使用 SnakeYAML 或 Hocon
    }
    
    return AppProperties()
}
