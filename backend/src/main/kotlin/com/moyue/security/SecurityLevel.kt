package com.moyue.security
 
enum class SecurityLevel(
    val displayName: String,
    val description: String,
    val riskLevel: Int
) {
    STANDARD(
        displayName = "标准模式",
        description = "最安全，仅允许基本网络请求和 DOM 操作",
        riskLevel = 0
    ),
    COMPATIBLE(
        displayName = "兼容模式",
        description = "允许文件操作和 Socket，中等安全",
        riskLevel = 1
    ),
    TRUSTED(
        displayName = "信任模式",
        description = "允许反射等高级功能，最低安全",
        riskLevel = 2
    );
    
    companion object {
        /**
         * 根据名称查找安全级别
         */
        fun fromName(name: String): SecurityLevel? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
        
        /**
         * 判断 level1 是否比 level2 风险更高
         */
        fun isHigherThan(level1: SecurityLevel, level2: SecurityLevel): Boolean {
            return level1.riskLevel > level2.riskLevel
        }
        
        /**
         * 判断两个级别是否相同
         */
        fun isSameLevel(level1: SecurityLevel, level2: SecurityLevel): Boolean {
            return level1 == level2
        }
    }
    
    /**
     * 是否可以升级（允许升级到更高风险级别）
     */
    fun canUpgrade(): Boolean {
        return this != TRUSTED
    }
    
    /**
     * 是否可以降级（允许降级到更低风险级别）
     */
    fun canDowngrade(): Boolean {
        return this != STANDARD
    }
    
    /**
     * 获取下一级别的枚举值
     */
    fun nextLevel(): SecurityLevel? {
        return values().getOrNull(this.ordinal + 1)
    }
    
    /**
     * 获取上一级别的枚举值
     */
    fun previousLevel(): SecurityLevel? {
        return values().getOrNull(this.ordinal - 1)
    }
}
