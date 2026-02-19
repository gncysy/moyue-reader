package com.moyue.controller

import com.moyue.security.SecurityLevel
import com.moyue.security.SecurityPolicy
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/security")
class SecurityController {
    
    private var currentPolicy: SecurityPolicy = SecurityPolicy.fromLevel(SecurityLevel.STANDARD)
    
    @GetMapping("/policy")
    fun getPolicy(): SecurityPolicy {
        return currentPolicy
    }
    
    @PostMapping("/policy")
    fun setPolicy(@RequestParam level: String, @RequestParam(required = false) confirmed: Boolean): Map<String, Any> {
        val newLevel = try {
            SecurityLevel.valueOf(level.uppercase())
        } catch (e: Exception) {
            return mapOf("success" to false, "message" to "无效的安全级别")
        }
        
        if (newLevel == SecurityLevel.TRUSTED && confirmed != true) {
            return mapOf("success" to false, "message" to "信任模式需要用户确认")
        }
        
        currentPolicy = SecurityPolicy.fromLevel(newLevel)
        return mapOf("success" to true, "policy" to currentPolicy)
    }
}
