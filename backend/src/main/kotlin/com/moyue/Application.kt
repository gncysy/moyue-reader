package com.moyue
 
import com.moyue.config.configureKoin
import com.moyue.config.configureLogging
import com.moyue.config.configureRouting
import com.moyue.config.configureSerialization
import com.moyue.config.configureStatusPages
import com.moyue.config.configureWebSockets
import com.moyue.config.configureCORS
import com.moyue.config.configureCompression
import com.moyue.config.initializeApplication
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
 
/**
 * 墨阅后端主应用入口
 * 使用 Ktor + Netty 框架
 */
fun main() {
    val logger = LoggerFactory.getLogger("Main")
    
    // 获取环境变量
    val port = System.getenv("SERVER_PORT")?.toInt() ?: 0
    val host = System.getenv("SERVER_ADDRESS") ?: "127.0.0.1"
    
    try {
        embeddedServer(Netty, port = port, host = host, module = Application::module)
            .start(wait = true)
            
    } catch (e: Exception) {
        logger.error("应用启动失败！", e)
        System.exit(1)
    }
}
 
/**
 * Ktor 应用模块配置
 */
fun Application.module() {
    val logger = LoggerFactory.getLogger("Module")
    
    // 初始化应用（创建工作目录、加载数据库等）
    initializeApplication()
    
    // 配置日志
    configureLogging()
    
    // 配置依赖注入（Koin）
    configureKoin()
    
    // 配置序列化
    configureSerialization()
    
    // 配置状态页面（错误处理）
    configureStatusPages()
    
    // 配置 CORS
    configureCORS()
    
    // 配置压缩
    configureCompression()
    
    // 配置 WebSocket
    configureWebSockets()
    
    // 配置路由
    configureRouting()
    
    logger.info("墨阅后端模块配置完成")
}
