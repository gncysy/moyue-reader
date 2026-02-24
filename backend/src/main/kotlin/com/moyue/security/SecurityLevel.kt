package com.moyue.security
 
/**
 * 安全等级枚举
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 安全等级说明：
 * - TRUSTED: 受信模式，允许所有操作（仅限可信环境）
 * - STANDARD: 标准模式，安全性与功能性平衡（推荐）
 * - COMPATIBLE: 兼容模式，最大程度兼容书源（安全性较低）
 *
 * @author Moyue Team
 * @since 4.0.3
 */
enum class SecurityLevel(
    /**
     * 等级名称
     */
    val name: String,
    
    /**
     * 描述
     */
    val description: String,
    
    /**
     * 是否允许网络访问
     */
    val allowsNetwork: Boolean,
    
    /**
     * 是否允许文件系统访问
     */
    val allowsFileSystem: Boolean,
    
    /**
     * 是否允许系统命令执行
     */
    val allowsSystem: Boolean,
    
    /**
     * 是否允许反射
     */
    val allowsReflection: Boolean,
    
    /**
     * 是否允许原生代码加载
     */
    val allowsNative: Boolean,
    
    /**
     * 最大执行时间（秒）
     */
    val maxExecutionTime: Long,
    
    /**
     * 是否启用沙箱
     */
    val sandboxEnabled: Boolean
) {
    
    /**
     * 受信模式
     * 允许所有操作，无安全限制
     */
    TRUSTED(
        name = "trusted",
        description = "受信模式 - 允许所有操作（仅限可信环境）",
        allowsNetwork = true,
        allowsFileSystem = true,
        allowsSystem = true,
        allowsReflection = true,
        allowsNative = true,
        maxExecutionTime = Long.MAX_VALUE,
        sandboxEnabled = false
    ),
    
    /**
     * 标准模式
     * 安全性与功能性平衡
     */
    STANDARD(
        name = "standard",
        description = "标准模式 - 安全性与功能性平衡（推荐）",
        allowsNetwork = true,
        allowsFileSystem = false,
        allowsSystem = false,
        allowsReflection = false,
        allowsNative = false,
        maxExecutionTime = 30,
        sandboxEnabled = true
    ),
    
    /**
     * 兼容模式
     * 最大程度兼容书源，安全性较低
     */
    COMPATIBLE(
        name = "compatible",
        description = "兼容模式 - 最大程度兼容书源（安全性较低）",
        allowsNetwork = true,
        allowsFileSystem = true,
        allowsSystem = false,
        allowsReflection = true,
        allowsNative = false,
        maxExecutionTime = 60,
        sandboxEnabled = true
    );
    
    companion object {
        private val LEVELS = values().associateBy { it.name.uppercase() }
        
        /**
         * 根据名称获取安全等级
         */
        fun fromName(name: String): SecurityLevel {
            return LEVELS[name.uppercase()] ?: STANDARD
        }
        
        /**
         * 获取所有安全等级
         */
        fun allLevels(): List<SecurityLevel> {
            return values().toList()
        }
    }
}
