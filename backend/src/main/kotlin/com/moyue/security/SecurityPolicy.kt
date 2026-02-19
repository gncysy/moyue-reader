package com.moyue.security

import java.nio.file.Paths

data class SecurityPolicy(
    val level: SecurityLevel,
    val allowFile: Boolean = level == SecurityLevel.COMPATIBLE || level == SecurityLevel.TRUSTED,
    val allowSocket: Boolean = level == SecurityLevel.COMPATIBLE || level == SecurityLevel.TRUSTED,
    val allowReflection: Boolean = level == SecurityLevel.TRUSTED,
    val timeoutMs: Long = 30000,
    val maxStackDepth: Int = 10000,
    val maxHttpConnections: Int = 5,
    val maxFileSize: Long = 10 * 1024 * 1024,
    val sandboxRoot: String = System.getProperty("user.home") + "/MoyueData"
) {
    companion object {
        fun fromLevel(level: SecurityLevel): SecurityPolicy = when (level) {
            SecurityLevel.STANDARD -> SecurityPolicy(level)
            SecurityLevel.COMPATIBLE -> SecurityPolicy(level)
            SecurityLevel.TRUSTED -> SecurityPolicy(level)
        }
    }
    
    init {
        java.nio.file.Files.createDirectories(Paths.get(sandboxRoot))
    }
}
