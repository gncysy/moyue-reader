package com.moyue.exception
 
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.time.LocalDateTime
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
 
/**
 * 全局异常处理器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 统一异常处理
 * - 友好的错误响应
 * - 详细的错误日志
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    // ==================== 业务异常 ====================
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("业务异常: ${ex.code} - ${ex.message}")
        
        return ResponseEntity.status(ex.httpStatus)
            .body(
                ErrorResponse(
                    code = ex.code,
                    message = ex.message ?: "业务处理失败",
                    details = ex.details,
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理实体不存在异常
     */
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("实体不存在: ${ex.message}")
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "ENTITY_NOT_FOUND",
                    message = ex.message ?: "请求的资源不存在",
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    // ==================== 参数验证异常 ====================
    
    /**
     * 处理 @Valid 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn("参数验证失败: ${ex.bindingResult.fieldErrors}")
        
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "验证失败")
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "VALIDATION_FAILED",
                    message = "参数验证失败",
                    details = mapOf("errors" to errors),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理 @Validated 参数验证异常
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        logger.warn("约束验证失败: ${ex.constraintViolations}")
        
        val errors = ex.constraintViolations.associate { 
            it.propertyPath.toString() to it.message
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "CONSTRAINT_VIOLATION",
                    message = "约束验证失败",
                    details = mapOf("errors" to errors),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(ex: BindException): ResponseEntity<ErrorResponse> {
        logger.warn("绑定失败: ${ex.fieldErrors}")
        
        val errors = ex.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "绑定失败")
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "BIND_FAILED",
                    message = "参数绑定失败",
                    details = mapOf("errors" to errors),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    // ==================== HTTP 异常 ====================
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn("参数类型不匹配: ${ex.name} -> ${ex.value}")
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "TYPE_MISMATCH",
                    message = "参数类型不匹配: ${ex.name}",
                    details = mapOf(
                        "expectedType" to (ex.requiredType?.simpleName),
                        "actualValue" to ex.value
                    ),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFoundException(ex: NoHandlerFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("请求路径不存在: ${ex.requestURL}")
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "NOT_FOUND",
                    message = "请求路径不存在: ${ex.requestURL}",
                    details = mapOf(
                        "method" to ex.httpMethod,
                        "url" to ex.requestURL
                    ),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    // ==================== 系统异常 ====================
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("非法参数: ${ex.message}")
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "ILLEGAL_ARGUMENT",
                    message = ex.message ?: "非法参数",
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.warn("非法状态: ${ex.message}")
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "ILLEGAL_STATE",
                    message = ex.message ?: "非法状态",
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理安全异常
     */
    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(ex: SecurityException): ResponseEntity<ErrorResponse> {
        logger.error("安全异常: ${ex.message}")
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    code = "SECURITY_ERROR",
                    message = ex.message ?: "安全检查失败",
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("未捕获的异常", ex)
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "服务器内部错误",
                    details = mapOf(
                        "type" to ex.javaClass.simpleName,
                        "message" to ex.message
                    ),
                    timestamp = LocalDateTime.now()
                )
            )
    }
    
    // ==================== 响应数据类 ====================
    
    /**
     * 错误响应
     */
    data class ErrorResponse(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null,
        val timestamp: LocalDateTime
    )
}
