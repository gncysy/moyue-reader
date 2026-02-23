package com.moyue.routing
 
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
 
/**
 * WebSocket 路由
 * 支持实时通信、进度推送等
 */
fun Route.webSocketRoutes() {
    val logger = KotlinLogging.logger {}
    
    // 存储活跃的 WebSocket 会话
    val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    
    /**
     * WebSocket 连接端点
     * WS /api/ws
     */
    webSocket("/api/ws") {
        val sessionId = this.call.request.queryParameters["session"] ?: "unknown"
        
        logger.info { "WebSocket 连接建立: $sessionId" }
        sessions[sessionId] = this
        
        // 发送欢迎消息
        send(Frame.Text("Welcome to Moyue WebSocket! Session: $sessionId"))
        
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug { "收到消息: $sessionId - $text" }
                        
                        // 处理消息
                        handleWebSocketMessage(sessionId, text, this)
                    }
                    is Frame.Close -> {
                        logger.info { "WebSocket 关闭: $sessionId" }
                        sessions.remove(sessionId)
                    }
                    else -> {
                        logger.warn { "未知帧类型: $sessionId - ${frame.frameType}" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "WebSocket 异常: $sessionId" }
        } finally {
            sessions.remove(sessionId)
            logger.info { "WebSocket 断开: $sessionId" }
        }
    }
    
    /**
     * 处理 WebSocket 消息
     */
    private suspend fun handleWebSocketMessage(
        sessionId: String,
        message: String,
        session: DefaultWebSocketServerSession
    ) {
        try {
            // 解析消息
            val parts = message.split(":", limit = 2)
            val type = parts.getOrNull(0) ?: "unknown"
            val payload = parts.getOrNull(1) ?: ""
            
            when (type) {
                "ping" -> {
                    session.send(Frame.Text("pong:${System.currentTimeMillis()}"))
                }
                "subscribe" -> {
                    // 订阅某个频道
                    session.send(Frame.Text("subscribed:$payload"))
                }
                "unsubscribe" -> {
                    // 取消订阅
                    session.send(Frame.Text("unsubscribed:$payload"))
                }
                else -> {
                    session.send(Frame.Text("error:Unknown message type: $type"))
                }
            }
        } catch (e: Exception) {
            session.send(Frame.Text("error:${e.message}"))
        }
    }
    
    /**
     * 广播消息到所有会话
     */
    suspend fun broadcast(message: String) {
        sessions.values.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.error(e) { "广播消息失败" }
            }
        }
    }
    
    /**
     * 发送消息到指定会话
     */
    suspend fun sendToSession(sessionId: String, message: String) {
        sessions[sessionId]?.send(Frame.Text(message))
    }
}
