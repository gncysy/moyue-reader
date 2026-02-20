package com.moyue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import java.io.File

@SpringBootApplication
@EnableAsync
@EnableScheduling
class MoyueApplication

fun main(args: Array<String>) {
    val context = runApplication<MoyueApplication>(*args)
    // 程序启动后，端口号会由 PortLogger 打印
}

/**
 * 监听服务器启动事件，打印实际端口号
 */
@Component
class PortLogger : ApplicationListener<WebServerInitializedEvent> {
    
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        println("========================================")
        println("🚀 墨阅后端启动成功！")
        println("📌 实际端口: $port")
        println("🌐 健康检查: http://localhost:$port/api/health")
        println("========================================")
        
        // 可选：把端口号写入文件，供前端读取
        try {
            File(System.getProperty("user.dir"), "backend-port.txt").writeText(port.toString())
        } catch (e: Exception) {
            // 忽略
        }
    }
}
