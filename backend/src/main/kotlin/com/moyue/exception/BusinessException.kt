package com.moyue.exception
 
import org.springframework.http.HttpStatus
 
/**
 * 业务异常
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 用于处理业务逻辑中的异常情况
 *
 * @author Moyue Team
 * @since 4.0.3
 */
class BusinessException(
    /**
     * 错误码
     */
    val code: String,
    
    /**
     * 错误消息
     */
    override val message: String?,
    
    /**
     * HTTP 状态码
     */
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    
    /**
     * 错误详情
     */
    val details: Map<String, Any>? = null,
    
    /**
     * 原始异常
     */
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    companion object {
        // ==================== 通用错误码 ====================
        
        /**
         * 参数错误
         */
        fun invalidParameter(paramName: String, reason: String? = null): BusinessException {
            return BusinessException(
                code = "INVALID_PARAMETER",
                message = "参数错误: $paramName${reason?.let { " - $it" } ?: ""}",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
        
        /**
         * 资源不存在
         */
        fun notFound(resource: String, id: String? = null): BusinessException {
            return BusinessException(
                code = "NOT_FOUND",
                message = if (id != null) "$resource 不存在: $id" else "$resource 不存在",
                httpStatus = HttpStatus.NOT_FOUND
            )
        }
        
        /**
         * 资源已存在
         */
        fun alreadyExists(resource: String, id: String? = null): BusinessException {
            return BusinessException(
                code = "ALREADY_EXISTS",
                message = if (id != null) "$resource 已存在: $id" else "$resource 已存在",
                httpStatus = HttpStatus.CONFLICT
            )
        }
        
        /**
         * 操作失败
         */
        fun operationFailed(operation: String, reason: String? = null): BusinessException {
            return BusinessException(
                code = "OPERATION_FAILED",
                message = "$operation 失败${reason?.let { ": $it" } ?: ""}",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
        
        // ==================== 书籍相关 ====================
        
        /**
         * 书籍不存在
         */
        fun bookNotFound(id: String): BusinessException {
            return notFound("书籍", id)
        }
        
        /**
         * 书籍已存在
         */
        fun bookAlreadyExists(bookUrl: String): BusinessException {
            return alreadyExists("书籍", bookUrl)
        }
        
        /**
         * 章节不存在
         */
        fun chapterNotFound(bookId: String, index: Int): BusinessException {
            return notFound("章节", "$bookId#$index")
        }
        
        /**
         * 章节内容获取失败
         */
        fun chapterContentFailed(chapterUrl: String, reason: String? = null): BusinessException {
            return operationFailed("获取章节内容", reason ?: chapterUrl)
        }
        
        // ==================== 书源相关 ====================
        
        /**
         * 书源不存在
         */
        fun sourceNotFound(sourceId: String): BusinessException {
            return notFound("书源", sourceId)
        }
        
        /**
         * 书源已存在
         */
        fun sourceAlreadyExists(sourceId: String): BusinessException {
            return alreadyExists("书源", sourceId)
        }
        
        /**
         * 书源不可用
         */
        fun sourceUnavailable(sourceId: String, reason: String? = null): BusinessException {
            return BusinessException(
                code = "SOURCE_UNAVAILABLE",
                message = "书源不可用: $sourceId${reason?.let { " - $it" } ?: ""}",
                httpStatus = HttpStatus.BAD_GATEWAY
            )
        }
        
        /**
         * 书源规则无效
         */
        fun sourceRuleInvalid(ruleType: String, reason: String? = null): BusinessException {
            return BusinessException(
                code = "SOURCE_RULE_INVALID",
                message = "书源规则无效: $ruleType${reason?.let { " - $it" } ?: ""}",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
        
        /**
         * 书源搜索失败
         */
        fun sourceSearchFailed(sourceId: String, reason: String? = null): BusinessException {
            return operationFailed("书源搜索", reason ?: sourceId)
        }
        
        // ==================== 网络相关 ====================
        
        /**
         * 网络请求失败
         */
        fun networkError(url: String, reason: String? = null): BusinessException {
            return BusinessException(
                code = "NETWORK_ERROR",
                message = "网络请求失败: $url${reason?.let { " - $it" } ?: ""}",
                httpStatus = HttpStatus.BAD_GATEWAY
            )
        }
        
        /**
         * 网络超时
         */
        fun networkTimeout(url: String): BusinessException {
            return BusinessException(
                code = "NETWORK_TIMEOUT",
                message = "网络请求超时: $url",
                httpStatus = HttpStatus.GATEWAY_TIMEOUT
            )
        }
        
        // ==================== 安全相关 ====================
        
        /**
         * 未授权
         */
        fun unauthorized(): BusinessException {
            return BusinessException(
                code = "UNAUTHORIZED",
                message = "未授权访问",
                httpStatus = HttpStatus.UNAUTHORIZED
            )
        }
        
        /**
         * 禁止访问
         */
        fun forbidden(): BusinessException {
            return BusinessException(
                code = "FORBIDDEN",
                message = "禁止访问",
                httpStatus = HttpStatus.FORBIDDEN
            )
        }
        
        /**
         * Token 无效
         */
        fun invalidToken(): BusinessException {
            return BusinessException(
                code = "INVALID_TOKEN",
                message = "无效的访问令牌",
                httpStatus = HttpStatus.UNAUTHORIZED
            )
        }
        
        /**
         * Token 过期
         */
        fun tokenExpired(): BusinessException {
            return BusinessException(
                code = "TOKEN_EXPIRED",
                message = "访问令牌已过期",
                httpStatus = HttpStatus.UNAUTHORIZED
            )
        }
        
        /**
         * 权限不足
         */
        fun insufficientPermission(): BusinessException {
            return BusinessException(
                code = "INSUFFICIENT_PERMISSION",
                message = "权限不足",
                httpStatus = HttpStatus.FORBIDDEN
            )
        }
        
        // ==================== 缓存相关 ====================
        
        /**
         * 缓存不存在
         */
        fun cacheNotFound(key: String): BusinessException {
            return notFound("缓存", key)
        }
        
        /**
         * 缓存操作失败
         */
        fun cacheOperationFailed(operation: String, key: String? = null): BusinessException {
            return operationFailed("缓存$operation", key)
        }
        
        // ==================== 文件相关 ====================
        
        /**
         * 文件不存在
         */
        fun fileNotFound(path: String): BusinessException {
            return notFound("文件", path)
        }
        
        /**
         * 文件上传失败
         */
        fun fileUploadFailed(reason: String): BusinessException {
            return operationFailed("文件上传", reason)
        }
        
        /**
         * 文件格式不支持
         */
        fun fileFormatNotSupported(format: String): BusinessException {
            return BusinessException(
                code = "FILE_FORMAT_NOT_SUPPORTED",
                message = "不支持的文件格式: $format",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
        
        /**
         * 文件过大
         */
        fun fileTooLarge(maxSize: String): BusinessException {
            return BusinessException(
                code = "FILE_TOO_LARGE",
                message = "文件过大，最大支持 $maxSize",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
        
        // ==================== 脚本相关 ====================
        
        /**
         * 脚本执行失败
         */
        fun scriptExecutionFailed(reason: String): BusinessException {
            return operationFailed("脚本执行", reason)
        }
        
        /**
         * 脚本语法错误
         */
        fun scriptSyntaxError(error: String): BusinessException {
            return BusinessException(
                code = "SCRIPT_SYNTAX_ERROR",
                message = "脚本语法错误: $error",
                httpStatus = HttpStatus.BAD_REQUEST
            )
        }
        
        /**
         * 脚本执行超时
         */
        fun scriptExecutionTimeout(): BusinessException {
            return BusinessException(
                code = "SCRIPT_EXECUTION_TIMEOUT",
                message = "脚本执行超时",
                httpStatus = HttpStatus.REQUEST_TIMEOUT
            )
        }
    }
}
