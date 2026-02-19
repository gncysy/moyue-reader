package com.moyue.controller

import com.moyue.model.BookSource
import com.moyue.service.SourceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/sources")
class SourceController(
    private val sourceService: SourceService
) {

    /**
     * 获取所有书源
     */
    @GetMapping
    fun getAllSources(
        @RequestParam(required = false) enabledOnly: Boolean = false
    ): List<BookSource> {
        return if (enabledOnly) {
            sourceService.getEnabledSources()
        } else {
            sourceService.getAllSources()
        }
    }

    /**
     * 获取单个书源详情
     */
    @GetMapping("/{id}")
    fun getSource(@PathVariable id: String): ResponseEntity<BookSource> {
        return sourceService.getSourceById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    /**
     * 创建书源
     */
    @PostMapping
    fun createSource(@RequestBody source: BookSource): BookSource {
        return sourceService.saveSource(source)
    }

    /**
     * 更新书源
     */
    @PutMapping("/{id}")
    fun updateSource(
        @PathVariable id: String,
        @RequestBody source: BookSource
    ): ResponseEntity<BookSource> {
        return if (sourceService.getSourceById(id) != null) {
            ResponseEntity.ok(sourceService.saveSource(source.copy(id = id)))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 删除书源
     */
    @DeleteMapping("/{id}")
    fun deleteSource(@PathVariable id: String): ResponseEntity<Void> {
        return if (sourceService.deleteSource(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 批量导入书源（JSON 字符串）
     */
    @PostMapping("/import")
    fun importSources(@RequestBody json: String): Map<String, Any> {
        val count = sourceService.importSources(json)
        return mapOf(
            "success" to true,
            "imported" to count,
            "message" to "成功导入 $count 个书源"
        )
    }

    /**
     * 批量导入书源（文件上传）
     */
    @PostMapping("/import/file")
    fun importSourcesFromFile(@RequestParam file: MultipartFile): Map<String, Any> {
        val json = String(file.bytes)
        val count = sourceService.importSources(json)
        return mapOf(
            "success" to true,
            "imported" to count,
            "filename" to file.originalFilename,
            "message" to "成功导入 $count 个书源"
        )
    }

    /**
     * 导出书源
     */
    @PostMapping("/export")
    fun exportSources(@RequestBody sourceIds: List<String>): ResponseEntity<String> {
        val json = sourceService.exportSources(sourceIds)
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=sources.json")
            .body(json)
    }

    /**
     * 导出所有书源
     */
    @GetMapping("/export/all")
    fun exportAllSources(): ResponseEntity<String> {
        val allSources = sourceService.getAllSources()
        val json = sourceService.exportSources(allSources.mapNotNull { it.id })
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=all_sources.json")
            .body(json)
    }

    /**
     * 测试书源
     */
    @PostMapping("/{id}/test")
    fun testSource(
        @PathVariable id: String,
        @RequestParam(defaultValue = "standard") securityLevel: String
    ): Map<String, Any> {
        return sourceService.testSource(id, securityLevel)
    }

    /**
     * 批量测试书源
     */
    @PostMapping("/test/batch")
    fun testSources(
        @RequestBody sourceIds: List<String>,
        @RequestParam(defaultValue = "standard") securityLevel: String
    ): List<Map<String, Any>> {
        return sourceIds.map { id ->
            mapOf(
                "id" to id,
                "result" to sourceService.testSource(id, securityLevel)
            )
        }
    }

    /**
     * 搜索书源（根据关键词搜索书源本身，不是用书源搜书）
     */
    @GetMapping("/search")
    fun searchSources(
        @RequestParam keyword: String
    ): List<BookSource> {
        return sourceService.searchSources(keyword)
    }

    /**
     * 启用书源
     */
    @PostMapping("/{id}/enable")
    fun enableSource(@PathVariable id: String): ResponseEntity<BookSource> {
        val source = sourceService.getSourceById(id) ?: return ResponseEntity.notFound().build()
        val updatedSource = source.copy(enabled = true)
        return ResponseEntity.ok(sourceService.saveSource(updatedSource))
    }

    /**
     * 禁用车源
     */
    @PostMapping("/{id}/disable")
    fun disableSource(@PathVariable id: String): ResponseEntity<BookSource> {
        val source = sourceService.getSourceById(id) ?: return ResponseEntity.notFound().build()
        val updatedSource = source.copy(enabled = false)
        return ResponseEntity.ok(sourceService.saveSource(updatedSource))
    }

    /**
     * 批量启用书源
     */
    @PostMapping("/enable/batch")
    fun enableSources(@RequestBody sourceIds: List<String>): Map<String, Any> {
        val count = sourceService.batchSetEnabled(sourceIds, true)
        return mapOf(
            "success" to true,
            "enabled" to count
        )
    }

    /**
     * 批量禁用车源
     */
    @PostMapping("/disable/batch")
    fun disableSources(@RequestBody sourceIds: List<String>): Map<String, Any> {
        val count = sourceService.batchSetEnabled(sourceIds, false)
        return mapOf(
            "success" to true,
            "disabled" to count
        )
    }

    /**
     * 获取书源分组列表
     */
    @GetMapping("/groups")
    fun getSourceGroups(): List<String> {
        return sourceService.getAllSources()
            .mapNotNull { it.group }
            .distinct()
            .sorted()
    }

    /**
     * 按分组获取书源
     */
    @GetMapping("/group/{group}")
    fun getSourcesByGroup(@PathVariable group: String): List<BookSource> {
        return sourceService.getSourcesByGroup(group)
    }

    /**
     * 兼容性分析
     */
    @GetMapping("/analyze")
    fun analyzeCompatibility(): Map<String, Any> {
        return sourceService.analyzeCompatibility()
    }
}
