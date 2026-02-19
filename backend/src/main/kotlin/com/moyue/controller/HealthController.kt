package com.moyue.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {
    
    @GetMapping("/api/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "ok",
            "timestamp" to LocalDateTime.now().toString(),
            "version" to "0.1.0"
        )
    }
}
