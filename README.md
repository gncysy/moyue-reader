```markdown
# 墨阅 (Moyue)

<div align="center">
  <p><strong>基于 Legado 重构的跨平台桌面阅读器</strong></p>
  <p>
    <a href="https://github.com/gncysy/moyue-reader/releases">
      <img src="https://img.shields.io/github/v/release/gncysy/moyue-reader" alt="Release">
    </a>
    <a href="https://github.com/gncysy/moyue-reader/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/gncysy/moyue-reader" alt="License">
    </a>
    <a href="https://github.com/gncysy/moyue-reader/issues">
      <img src="https://img.shields.io/github/issues/gncysy/moyue-reader" alt="Issues">
    </a>
  </p>
</div>

## 项目简介

墨阅是一个跨平台桌面阅读器，将 Android 开源项目 Legado 的核心功能重构为桌面应用。项目采用前后端分离架构，后端基于 Spring Boot + Kotlin，前端基于 Electron + Vue 3，旨在提供与 Legado 相近的书源兼容性和阅读体验。

## 功能特性

### 📚 书架管理
- 书籍信息的增删改查
- 阅读进度自动同步
- 封面缓存与清理
- 自定义分组

### 🔍 书源系统
- 支持导入/导出 Legado 格式书源（JSON）
- 书源启用/禁用
- 书源测试与调试
- 基础兼容性分析

### 📖 阅读器
- **翻页模式**：覆盖、仿真、滑动、滚动
- **排版设置**：字体、字号、行距、对齐方式
- **主题切换**：日间、夜间、护眼
- **目录导航**：快速跳转章节
- **书签管理**：添加/删除书签

### 🛡️ 安全沙箱
| 模式 | 兼容性 | 限制 |
| --- | --- | --- |
| 标准模式 | ~85% | 禁止文件/Socket/反射操作 |
| 兼容模式 | ~95% | 允许文件/Socket，禁止反射 |
| 信任模式 | ~98% | 仅超时保护，需手动确认 |

### 🔊 TTS 朗读
- 系统 TTS 支持（Windows SAPI5 / macOS AVSpeech）
- 语速调节
- 后台播放

### 🔄 WebDAV 同步
- 书架备份与恢复
- 阅读进度同步
- 自动/手动同步模式

### 📄 本地书籍
- **TXT**：自动分章（正则匹配章节标题）
- **EPUB**：解析目录、封面、正文

## 技术栈

### 后端
- Kotlin 1.9
- Spring Boot 3.2
- Rhino 1.7.15
- OkHttp
- Jsoup
- H2 / SQLite

### 前端
- Electron 28
- Vue 3.4
- TypeScript 5.3
- Element Plus
- Pinia
- Vue Router

## 快速开始

### 环境要求
- JDK 17 (Eclipse Temurin)
- Node.js 18+
- Git

### 克隆项目
```bash
git clone https://github.com/gncysy/moyue-reader.git
cd moyue-reader
```

启动后端

```bash
cd backend
./gradlew bootRun
```

启动前端

新开终端窗口，执行：

```bash
cd frontend
npm install
npm run electron:dev
```

下载安装

访问 Releases 页面下载对应平台的安装包：

· Windows：.exe 安装包
· macOS：.dmg 安装包
· Linux：.AppImage 或 .deb 包

贡献指南

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
3. 提交更改 (git commit -m 'feat: add some feature')
4. 推送到分支 (git push origin feature/AmazingFeature)
5. 打开 Pull Request

致谢

本项目基于以下开源项目：

· Legado - Android 开源阅读器
· Reader - 阅读3服务器版
· Rhino - JavaScript 引擎
· Electron
· Spring Boot
· Vue.js

许可证

GPL-3.0 License © 2024 墨阅团队

联系方式

· 问题反馈：Issues

这个版本遵循了开源项目的标准格式[citation:4][citation:9]：
- **标题层级规范**：`#` 一级标题（项目名）、`##` 二级标题（各章节）、`###` 三级标题（功能分类）
- **表格格式正确**：使用 `|---|---|---|` 分隔线
- **代码块统一**：全部用 ``` 包裹，
