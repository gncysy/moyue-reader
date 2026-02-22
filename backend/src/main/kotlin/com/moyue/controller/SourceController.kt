package com.moyue.controller

import com.moyue.model.BookSource
import com.moyue.service.SourceService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/sources")
class SourceController(
    private val sourceService: SourceService
) {
    
    private val logger = LoggerFactory.getLogger(SourceController::class.java)
    
    /**
     * 获取所有书源
     */
    @GetMapping
    fun getAllSources(
        @RequestParam(required = false) enabledOnly: Boolean = false,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val sources = if (enabledOnly) {
                sourceService.getEnabledSources()
            } else {
                sourceService.getAllSources()
            }
            
            // 分页处理
            val start = page * size
            val end = (start + size).coerceAtMost(sources.size)
            val pagedSources = if (start < sources.size) {
                sources.subList(start, end)
            } else {
                emptyList()
            }
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "sources" to pagedSources,
                "total" to sources.size,
                "page" to page,
                "size" to size
            ))
        } catch (e: Exception) {
            logger.error("获取书源列表失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取单个书源详情
     */
    @GetMapping("/{id}")
    fun getSource(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.debug("获取书源详情: $id")
        return try {
            val source = sourceService.getSourceById(id)
            if (source != null) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "source" to source
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("获取书源失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 创建书源
     */
    @PostMapping
    fun createSource(@RequestBody source: BookSource): ResponseEntity<Map<String, Any>> {
        logger.info("创建书源: ${source.name}")
        return try {
            val saved = sourceService.saveSource(source)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf(
                "success" to true,
                "source" to saved,
                "message" to "书源创建成功"
            ))
        } catch (e: Exception) {
            logger.error("创建书源失败", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "创建失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 更新书源
     */
    @PutMapping("/{id}")
    fun updateSource(
        @PathVariable id: String,
        @RequestBody source: BookSource
    ): ResponseEntity<Map<String, Any>> {
        logger.info("更新书源: $id")
        return try {
            if (sourceService.getSourceById(id) != null) {
                val updated = sourceService.saveSource(source.copy(id = id))
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "source" to updated,
                    "message" to "书源更新成功"
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("更新书源失败: $id", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "更新失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 删除书源
     */
    @DeleteMapping("/{id}")
    fun deleteSource(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("删除书源: $id")
        return try {
            if (sourceService.deleteSource(id)) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "书源删除成功"
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("删除书源失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "删除失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量导入书源（JSON 字符串）
     */
    @PostMapping("/import")
    fun importSources(@RequestBody json: String): ResponseEntity<Map<String, Any>> {
        logger.info("导入书源（JSON）")
        return try {
            val result = sourceService.importSource(json)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "imported" to result.successCount,
                "failed" to result.failedCount,
                "total" to result.total,
                "message" to "成功导入 ${result.successCount} 个书源",
                "failedSources" to result.failed.map { it.name }
            ))
        } catch (e: Exception) {
            logger.error("导入书源失败", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "导入失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量导入书源（文件上传）
     */
    @PostMapping("/import/file")
    fun importSourcesFromFile(@RequestParam file: MultipartFile): ResponseEntity<Map<String, Any>> {
        logger.info("导入书源（文件）: ${file.originalFilename}")
        return try {
            val json = String(file.bytes)
            val result = sourceService.importSource(json)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "imported" to result.successCount,
                "failed" to result.failedCount,
                "total" to result.total,
                "filename" to file.originalFilename,
                "message" to "成功导入 ${result.successCount} 个书源",
                "failedSources" to result.failed.map { it.name }
            ))
        } catch (e: Exception) {
            logger.error("导入书源文件失败", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "导入失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 导出书源
     */
    @PostMapping("/export")
    fun exportSources(@RequestBody sourceIds: List<String>): ResponseEntity<String> {
        logger.info("导出书源: ${sourceIds.size} 个")
        val json = sourceService.exportSource(sourceIds)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sources.json")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(json)
    }
    
    /**
     * 导出所有书源
     */
    @GetMapping("/export/all")
    fun exportAllSources(): ResponseEntity<String> {
        logger.info("导出所有书源")
        val allSources = sourceService.getAllSources()
        val json = sourceService.exportSource(allSources.mapNotNull { it.id })
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all_sources.json")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(json)
    }
    
    /**
     * 测试书源
     */
    @PostMapping("/{id}/test")
    fun testSource(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("测试书源: $id")
        return try {
            val result = sourceService.testSource(id)
            ResponseEntity.ok(mapOf(
                "success" to result.success,
                "sourceName" to result.sourceName,
                "error" to result.error,
                "details" to result.details
            ))
        } catch (e: Exception) {
            logger.error("测试书源失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "error" to "测试失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量测试书源
     */
    @PostMapping("/test/batch")
    fun testSources(@RequestBody sourceIds: List<String>): ResponseEntity<Map<String, Any>> {
        logger.info("批量测试书源: ${sourceIds.size} 个")
        return try {
            val results = sourceIds.map { id ->
                val result = sourceService.testSource(id)
                mapOf(
                    "id" to id,
                    "sourceName" to result.sourceName,
                    "success" to result.success,
                    "error" to result.error
                )
            }
            
            val successCount = results.count { it["success"] == true }
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "total" to results.size,
                "successCount" to successCount,
                "failedCount" to results.size - successCount,
                "results" to results
            ))
        } catch (e: Exception) {
            logger.error("批量测试书源失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "测试失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 搜索书源（根据关键词搜索书源本身）
     */
    @GetMapping("/search")
    fun searchSources(
        @RequestParam keyword: String
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("搜索书源: $keyword")
        return try {
            val sources = sourceService.searchSources(keyword)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "sources" to sources,
                "total" to sources.size,
                "keyword" to keyword
            ))
        } catch (e: Exception) {
            logger.error("搜索书源失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "搜索失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 启用书源
     */
    @PostMapping("/{id}/enable")
    fun enableSource(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("启用书源: $id")
        return try {
            val source = sourceService.getSourceById(id) ?: return ResponseEntity.notFound().build()
            val updated = source.copy(enabled = true)
            sourceService.saveSource(updated)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "source" to updated,
                "message" to "书源已启用"
            ))
        } catch (e: Exception) {
            logger.error("启用书源失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "启用失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 禁用书源
     */
    @PostMapping("/{id}/disable")
    fun disableSource(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("禁用书源: $id")
        return try {
            val source = sourceService.getSourceById(id) ?: return ResponseEntity.notFound().build()
            val updated = source.copy(enabled = false)
            sourceService.saveSource(updated)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "source" to updated,
                "message" to "书源已禁用"
            ))
        } catch (e: Exception) {
            logger.error("禁用书源失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "禁用失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量启用书源
     */
    @PostMapping("/enable/batch")
    fun enableSources(@RequestBody sourceIds: List<String>): ResponseEntity<Map<String, Any>> {
        logger.info("批量启用书源: ${sourceIds.size} 个")
        return try {
            val count = sourceService.batchSetEnabled(sourceIds, true)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "enabled" to count,
                "message" to "已启用 $count 个书源"
            ))
        } catch (e: Exception) {
            logger.error("批量启用书源失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "启用失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量禁用书源
     */
    @PostMapping("/disable/batch")
    fun disableSources(@RequestBody sourceIds: List<String>): ResponseEntity<Map<String, Any>> {
        logger.info("批量禁用书源: ${sourceIds.size} 个")
        return try {
            val count = sourceService.batchSetEnabled(sourceIds, false)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "disabled" to count,
                "message" to "已禁用 $count 个书源"
            ))
        } catch (e: Exception) {
            logger.error("批量禁用书源失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "禁用失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取书源分组列表
     */
    @GetMapping("/groups")
    fun getSourceGroups(): ResponseEntity<Map<String, Any>> {
        logger.debug("获取书源分组列表")
        return try {
            val groups = sourceService.getAllSources()
                .mapNotNull { it.group }
                .distinct()
                .sorted()
            ResponseEntity.ok(mapOf(
                "success" to true,
                "groups" to groups,
                "total" to groups.size
            ))
        } catch (e: Exception) {
            logger.error("获取书源分组失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 按分组获取书源
     */
    @GetMapping("/group/{group}")
    fun getSourcesByGroup(@PathVariable group: String): ResponseEntity<Map<String, Any>> {
        logger.debug("获取分组书源: $group")
        return try {
            val sources = sourceService.getSourcesByGroup(group)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "group" to group,
                "sources" to sources,
                "total" to sources.size
            ))
        } catch (e: Exception) {
            logger.error("获取分组书源失败: $group", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 获取书源统计
     */
    @GetMapping("/stats")
    fun getSourceStats(): ResponseEntity<Map<String, Any>> {
        logger.debug("获取书源统计")
        return try {
            val stats = sourceService.getSourceStats()
            ResponseEntity.ok(mapOf(
                "success" to true,
                "stats" to stats
            ))
        } catch (e: Exception) {
            logger.error("获取书源统计失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "获取失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 重置书源失败计数
     */
    @PostMapping("/{id}/reset-fail")
    fun resetSourceFailCount(@PathVariable id: String): ResponseEntity<Map<String, Any>> {
        logger.info("重置书源失败计数: $id")
        return try {
            if (sourceService.resetSourceFailCount(id)) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "失败计数已重置"
                ))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("重置失败计数失败: $id", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "重置失败: ${e.message}"
            ))
        }
    }
    
    /**
     * 批量重置失败计数
     */
    @PostMapping("/reset-fail/batch")
    fun resetAllSourceFailCount(): ResponseEntity<Map<String, Any>> {
        logger.info("批量重置失败计数")
        return try {
            val count = sourceService.resetAllSourceFailCount()
            ResponseEntity.ok(mapOf(
                "success" to true,
                "reset" to count,
                "message" to "已重置 $count 个书源的失败计数"
            ))
        } catch (e: Exception) {
            logger.error("批量重置失败计数失败", e)
            ResponseEntity.status(500).body(mapOf(
                "success" to false,
                "message" to "重置失败: ${e.message}"
            ))
        }
    }
}
