package com.moyue.model
 
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import org.jspecify.annotations.Nullable
import tools.jackson.annotation.JsonFormat
import tools.jackson.annotation.JsonIgnore
import tools.jackson.annotation.JsonProperty
import java.time.LocalDateTime
 
/**
 * 书源实体
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 支持 Legado 书源格式
 *
 * @property id 唯一标识（UUID）
 * @property sourceId 书源 ID（外部来源）
 * @property name 书源名称
 * @property icon 书源图标
 * @property url 书源主页 URL
 * @property author 书源作者
 * @property enabled 是否启用
 * @property weight 权重（搜索时排序）
 * @property bookSourceRuleId 关联的规则 ID
 * @property rules 书源规则
 * @property lastUsedAt 最后使用时间
 * @property lastCheckedAt 最后检查时间
 * @property checkStatus 检查状态
 * @property checkMessage 检查消息
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Entity
@Table(
    name = "book_sources",
    indexes = [
        Index(name = "idx_source_id", columnList = "source_id"),
        Index(name = "idx_enabled_weight", columnList = "enabled, weight DESC"),
        Index(name = "idx_last_used_at", columnList = "last_used_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_source_id", columnNames = ["source_id"])
    ]
)
data class BookSource(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    
    @Column(name = "source_id", unique = true, length = 100)
    @field:NotBlank(message = "书源 ID 不能为空")
    var sourceId: String = "",
    
    @Column(nullable = false, length = 200)
    @field:NotBlank(message = "书源名称不能为空")
    var name: String = "",
    
    @Column(length = 1000)
    @Nullable
    var icon: String? = null,
    
    @Column(length = 1000)
    @Nullable
    var url: String? = null,
    
    @Column(length = 200)
    @Nullable
    var author: String? = null,
    
    @Column(nullable = false)
    var enabled: Boolean = true,
    
    @Column(nullable = false)
    var weight: Int = 0,
    
    @Column(name = "book_source_rule_id", length = 100, nullable = true)
    @Nullable
    var bookSourceRuleId: String? = null,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_source_rule_id", insertable = false, updatable = false)
    @JsonIgnore  // 避免循环序列化
    @Nullable
    var rules: BookSourceRules? = null,
    
    @Column(name = "last_used_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Nullable
    var lastUsedAt: LocalDateTime? = null,
    
    @Column(name = "last_checked_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Nullable
    var lastCheckedAt: LocalDateTime? = null,
    
    @Column(name = "check_status", length = 20)
    @Nullable
    var checkStatus: String? = null,  // success, failed, unknown
    
    @Column(name = "check_message", columnDefinition = "TEXT")
    @Nullable
    var checkMessage: String? = null,
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 判断书源是否可用
     */
    @get:JsonProperty("isAvailable")
    val isAvailable: Boolean
        get() = enabled && checkStatus == "success"
    
    /**
     * 判断书源是否需要检查
     */
    fun needsCheck(): Boolean {
        return if (lastCheckedAt == null) {
            true
        } else {
            val hours = java.time.Duration.between(lastCheckedAt, LocalDateTime.now()).toHours()
            hours > 24  // 24 小时后重新检查
        }
    }
    
    /**
     * 更新使用时间
     */
    fun updateUsedTime() {
        lastUsedAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 更新检查状态
     */
    fun updateCheckStatus(status: String, message: String? = null) {
        checkStatus = status
        checkMessage = message
        lastCheckedAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * 从 JSON 字符串解析
         */
        fun fromJson(json: String): BookSource {
            return com.google.gson.Gson().fromJson(json, BookSource::class.java)
        }
    }
    
    // ==================== JPA 生命周期回调 ====================
    
    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }
    
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
