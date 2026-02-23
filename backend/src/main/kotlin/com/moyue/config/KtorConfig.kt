package com.moyue.config
 
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import mu.KotlinLogging
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.time.Duration
 
/**
 * 配置日志
 */
fun Application.configureLogging() {
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
    }
}
 
/**
 * 配置 Koin 依赖注入
 */
fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
 
/**
 * 配置序列化（JSON）
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
 
/**
 * 配置状态页面（错误处理）
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(mapOf(
                "success" to false,
                "message" to (cause.message ?: "内部服务器错误")
            ))
        }
    }
}
 
/**
 * 配置 CORS
 */
fun Application.configureCORS() {
    install(CORS) {
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
        anyHost() // 生产环境请限制域名
    }
}
 
/**
 * 配置压缩
 */
fun Application.configureCompression() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
        }
    }
}
 
/**
 * 配置 WebSocket
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
