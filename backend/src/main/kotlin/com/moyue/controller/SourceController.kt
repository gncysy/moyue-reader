package com.moyue.controller
 
import com.moyue.model.BookSource
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.jspecify.annotations.Nullable
import java.time.LocalDateTime
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
 
/**
 * 书源控制器
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 *
 * 功能：
 * - 书源 CRUD
 * - 书源导入/导出
 * - 书源搜索
 * - 书源检查
 * - 书源调试
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@RestController
@RequestMapping("/api/sources")
class SourceController(
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(SourceController::class.java)
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有书源（分页）
     */
    @GetMapping
    fun getAllSources(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<BookController.ApiResponse<Page<BookSource>>> {
        logger.debug("获取书源列表: page=$page, size=$size")
        
        val sources = sourceService.getAllSources(page, size)
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = sources,
                message = "获取成功"
            )
        )
    }
    
    /**
     * 获取启用的书源
     */
    @GetMapping("/enabled")
    fun getEnabledSources(): ResponseEntity<BookController.ApiResponse<List<BookSource>>> {
        logger.debug("获取启用的书源")
        
        val sources = sourceService.getEnabledSources()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = sources,
                message = "获取成功，共 ${sources.size} 个书源"
            )
        )
    }
    
    /**
     * 获取可用的书源
     */
    @GetMapping("/available")
    fun getAvailableSources(): ResponseEntity<BookController.ApiResponse<List<BookSource>>> {
        logger.debug("获取可用的书源")
        
        val sources = sourceService.getAvailableSources()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = sources,
                message = "获取成功，共 ${sources.size} 个可用书源"
            )
        )
    }
    
    /**
     * 根据书源 ID 查询
     */
    @GetMapping("/{sourceId}")
    fun getSource(@PathVariable sourceId: String): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.debug("获取书源详情: $sourceId")
        
        return try {
            val source = sourceService.getSourceById(sourceId)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = source,
                    message = "获取成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BookController.ApiResponse.error(
                    code = "SOURCE_NOT_FOUND",
                    message = "书源不存在: $sourceId"
                )
            )
        }
    }
    
    /**
     * 搜索书源
     */
    @GetMapping("/search")
    fun searchSources(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<BookController.ApiResponse<Page<BookSource>>> {
        logger.info("搜索书源: $keyword")
        
        val sources = sourceService.searchSources(keyword, page, size)
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = sources,
                message = "搜索完成，找到 ${sources.totalElements} 个书源"
            )
        )
    }
    
    /**
     * 获取书源统计
     */
    @GetMapping("/stats")
    fun getSourceStats(): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.debug("获取书源统计")
        
        val stats = sourceService.getSourceStats()
        return ResponseEntity.ok(
            BookController.ApiResponse.success(
                data = stats,
                message = "获取成功"
            )
        )
    }
    
    // ==================== 保存操作 ====================
    
    /**
     * 添加书源
     */
    @PostMapping
    fun addSource(@RequestBody @Valid request: AddSourceRequest): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.info("添加书源: ${request.name}")
        
        return try {
            val source = BookSource(
                sourceId = request.sourceId,
                name = request.name,
                url = request.url,
                author = request.author,
                enabled = request.enabled,
                weight = request.weight
            )
            
            val saved = sourceService.saveSource(source)
            ResponseEntity.status(HttpStatus.CREATED).body(
                BookController.ApiResponse.success(
                    data = saved,
                    message = "书源添加成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("添加书源失败", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "ADD_SOURCE_FAILED",
                    message = "添加失败: ${e.message}"
                )
            )
        } catch (e: Exception) {
            logger.error("添加书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "ADD_SOURCE_FAILED",
                    message = "添加失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 导入书源（JSON 文件）
     */
    @PostMapping("/import")
    fun importSource(@RequestParam file: MultipartFile): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.info("导入书源: ${file.originalFilename}")
        
        return try {
            val source = sourceService.importSource(file)
            ResponseEntity.status(HttpStatus.CREATED).body(
                BookController.ApiResponse.success(
                    data = source,
                    message = "书源导入成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("导入书源失败", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "IMPORT_SOURCE_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        } catch (e: Exception) {
            logger.error("导入书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "IMPORT_SOURCE_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量导入书源
     */
    @PostMapping("/import/batch")
    fun importSources(@RequestBody sources: List<BookSource>): ResponseEntity<BookController.ApiResponse<List<BookSource>>> {
        logger.info("批量导入书源: ${sources.size} 个")
        
        return try {
            val saved = sourceService.importSources(sources)
            ResponseEntity.status(HttpStatus.CREATED).body(
                BookController.ApiResponse.success(
                    data = saved,
                    message = "成功导入 ${saved.size} 个书源"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("批量导入书源失败", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                BookController.ApiResponse.error(
                    code = "IMPORT_SOURCES_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        } catch (e: Exception) {
            logger.error("批量导入书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "IMPORT_SOURCES_FAILED",
                    message = "导入失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新书源
     */
    @PutMapping("/{sourceId}")
    fun updateSource(
        @PathVariable sourceId: String,
        @RequestBody @Valid request: UpdateSourceRequest
    ): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.info("更新书源: $sourceId")
        
        return try {
            val existing = sourceService.getSourceById(sourceId)
            val updated = existing.copy(
                name = request.name ?: existing.name,
                url = request.url ?: existing.url,
                author = request.author ?: existing.author,
                enabled = request.enabled ?: existing.enabled,
                weight = request.weight ?: existing.weight
            )
            
            val saved = sourceService.updateSource(sourceId, updated)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = saved,
                    message = "书源更新成功"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BookController.ApiResponse.error(
                    code = "SOURCE_NOT_FOUND",
                    message = "书源不存在: $sourceId"
                )
            )
        } catch (e: Exception) {
            logger.error("更新书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "UPDATE_SOURCE_FAILED",
                    message = "更新失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 启用/禁用书源
     */
    @PatchMapping("/{sourceId}/toggle")
    fun toggleSource(
        @PathVariable sourceId: String,
        @RequestBody request: ToggleSourceRequest
    ): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.info("切换书源状态: $sourceId, enabled=${request.enabled}")
        
        return try {
            val source = sourceService.toggleSource(sourceId, request.enabled)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = source,
                    message = "书源状态已更新"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                BookController.ApiResponse.error(
                    code = "SOURCE_NOT_FOUND",
                    message = "书源不存在: $sourceId"
                )
            )
        } catch (e: Exception) {
            logger.error("切换书源状态失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "TOGGLE_SOURCE_FAILED",
                    message = "操作失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 更新书源权重
     */
    @PatchMapping("/{sourceId}/weight")
    fun updateWeight(
        @PathVariable sourceId: String,
        @RequestBody request: UpdateWeightRequest
    ): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("更新书源权重: $sourceId, weight=${request.weight}")
        
        return try {
            sourceService.updateWeight(sourceId, request.weight)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    message = "权重更新成功"
                )
            )
        } catch (e: Exception) {
            logger.error("更新权重失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "UPDATE_WEIGHT_FAILED",
                    message = "更新失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除书源
     */
    @DeleteMapping("/{sourceId}")
    fun deleteSource(@PathVariable sourceId: String): ResponseEntity<BookController.ApiResponse<Unit>> {
        logger.info("删除书源: $sourceId")
        
        return try {
            val success = sourceService.deleteSource(sourceId)
            if (success) {
                ResponseEntity.ok(
                    BookController.ApiResponse.success(
                        message = "书源删除成功"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BookController.ApiResponse.error(
                        code = "SOURCE_NOT_FOUND",
                        message = "书源不存在: $sourceId"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("删除书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "DELETE_SOURCE_FAILED",
                    message = "删除失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量删除书源
     */
    @DeleteMapping
    fun deleteSources(@RequestBody request: DeleteSourcesRequest): ResponseEntity<BookController.ApiResponse<Map<String, Any>>> {
        logger.info("批量删除书源: ${request.sourceIds.size} 个")
        
        return try {
            val count = sourceService.deleteSources(request.sourceIds)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = mapOf("deleted" to count),
                    message = "成功删除 $count 个书源"
                )
            )
        } catch (e: Exception) {
            logger.error("批量删除书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "DELETE_SOURCES_FAILED",
                    message = "删除失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 书源检查 ====================
    
    /**
     * 检查书源可用性
     */
    @PostMapping("/{sourceId}/check")
    fun checkSource(@PathVariable sourceId: String): ResponseEntity<BookController.ApiResponse<BookSource>> {
        logger.info("检查书源: $sourceId")
        
        return try {
            val source = sourceService.checkSource(sourceId)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = source,
                    message = "检查完成"
                )
            )
        } catch (e: Exception) {
            logger.error("检查书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "CHECK_SOURCE_FAILED",
                    message = "检查失败: ${e.message}"
                )
            )
        }
    }
    
    /**
     * 批量检查书源
     */
    @PostMapping("/check/batch")
    fun checkSources(@RequestBody request: CheckSourcesRequest): ResponseEntity<BookController.ApiResponse<List<BookSource>>> {
        logger.info("批量检查书源: ${request.sourceIds?.size ?: "all"}")
        
        return try {
            val sources = sourceService.checkSources(request.sourceIds)
            ResponseEntity.ok(
                BookController.ApiResponse.success(
                    data = sources,
                    message = "检查完成，共 ${sources.size} 个书源"
                )
            )
        } catch (e: Exception) {
            logger.error("批量检查书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                BookController.ApiResponse.error(
                    code = "CHECK_SOURCES_FAILED",
                    message = "检查失败: ${e.message}"
                )
            )
        }
    }
    
    // ==================== 书源导出 ====================
    
    /**
     * 导出书源（JSON）
     */
    @GetMapping("/{sourceId}/export")
    fun exportSource(@PathVariable sourceId: String): ResponseEntity<String> {
        logger.info("导出书源: $sourceId")
        
        return try {
            val json = sourceService.exportSource(sourceId)
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=source-$sourceId.json")
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(json)
        } catch (e: Exception) {
            logger.error("导出书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("""{"error": "导出失败: ${e.message}"}""")
        }
    }
    
    /**
     * 导出所有书源（JSON 数组）
     */
    @GetMapping("/export")
    fun exportAllSources(): ResponseEntity<String> {
        logger.info("导出所有书源")
        
        return try {
            val json = sourceService.exportAllSources()
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=sources.json")
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(json)
        } catch (e: Exception) {
            logger.error("导出书源失败", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("""{"error": "导出失败: ${e.message}"}""")
        }
    }
    
    // ==================== 请求/响应 DTO ====================
    
    /**
     * 添加书源请求
     */
    data class AddSourceRequest(
        @field:NotBlank(message = "书源 ID 不能为空")
        val sourceId: String,
        
        @field:NotBlank(message = "书源名称不能为空")
        val name: String,
        
        val url: String? = null,
        
        val author: String? = null,
        
        val enabled: Boolean = true,
        
        val weight: Int = 0
    )
    
    /**
     * 更新书源请求
     */
    data class UpdateSourceRequest(
        val name: String? = null,
        val url: String? = null,
        val author: String? = null,
        val enabled: Boolean? = null,
        val weight: Int? = null
    )
    
    /**
     * 切换书源状态请求
     */
    data class ToggleSourceRequest(
        val enabled: Boolean
    )
    
    /**
     * 更新权重请求
     */
    data class UpdateWeightRequest(
        val weight: Int
    )
    
    /**
     * 删除书源请求
     */
    data class DeleteSourcesRequest(
        val sourceIds: List<String>
    )
    
    /**
     * 检查书源请求
     */
    data class CheckSourcesRequest(
        val sourceIds: List<String>? = null
    )
}
