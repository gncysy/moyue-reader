package com.moyue.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@RestController
class HealthController {
    
    private val logger = LoggerFactory.getLogger(HealthController::class.java)
    
    @GetMapping("/api/health")
    fun health(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val uptime = System.currentTimeMillis() - MANAGEMENT_FACTORY.runtimeMXBean.uptime
        
        return mapOf(
            "status" to "ok",
            "timestamp" to LocalDateTime.now().toString(),
            "version" to System.getProperty("app.version", "1.0.0"),
            "system" to mapOf(
                "javaVersion" to System.getProperty("java.version"),
                "osName" to System.getProperty("os.name"),
                "osVersion" to System.getProperty("os.version"),
                "processors" to runtime.availableProcessors(),
                "uptime" to uptime,
                "timezone" to ZoneId.systemDefault().id
            ),
            "memory" to mapOf(
                "total" to "${runtime.totalMemory() / 1024 / 1024}MB",
                "free" to "${runtime.freeMemory() / 1024 / 1024}MB",
                "used" to "${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}MB",
                "max" to "${runtime.maxMemory() / 1024 / 1024}MB"
            ),
            "thread" to mapOf(
                "threadCount" to Thread.activeCount(),
                "peakThreadCount" to MANAGEMENT_FACTORY.threadMXBean.peakThreadCount
            )
        )
    }
    
    companion object {
        private val MANAGEMENT_FACTORY = java.lang.management.ManagementFactory
    }
}
