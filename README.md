# 墨阅 (Moyue)

<div align="center">
  <strong>基于 Legado 重构的跨平台桌面阅读器</strong>
  <br>
  <br>
  <a href="https://github.com/gncysy/moyue-reader/releases">
    <img src="https://img.shields.io/github/v/release/gncysy/moyue-reader" alt="Release">
  </a>
  <a href="https://github.com/gncysy/moyue-reader/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/gncysy/moyue-reader" alt="License">
  </a>
  <a href="https://github.com/gncysy/moyue-reader/issues">
    <img src="https://img.shields.io/github/issues/gncysy/moyue-reader" alt="Issues">
  </a>
</div>

## 简介

墨阅是一个跨平台桌面阅读器，将 Android 开源项目 [Legado](https://github.com/gedoor/legado) 的核心功能重构为桌面应用。项目采用前后端分离架构，后端基于 Spring Boot + Kotlin，前端基于 Electron + Vue 3，旨在提供与 Legado 相近的书源兼容性和阅读体验。

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
|------|--------|------|
| 标准模式 | ~85% | 禁止文件/Socket/反射操作 |
| 兼容模式 | ~95% | 允许文件/Socket，禁止反射 |
| 信任模式 | ~98% | 仅超时保护，需手动确认 |

### 🔊 TTS 朗读
- 系统 TTS 集成（Windows SAPI5 / macOS AVSpeech）
- 语速调节
- 后台播放支持

### 🔄 WebDAV 同步
- 书架备份与恢复
- 阅读进度多端同步
- 自动/手动同步模式

### 📄 本地书籍
- **TXT**：自动分章（正则匹配章节标题）
- **EPUB**：解析目录、封面、正文

## 技术架构

### 后端
- **框架**：Spring Boot 3.2 + Kotlin 1.9
- **JS引擎**：Rhino 1.7.15（与 Legado 版本一致）
- **网络**：OkHttp 4.12 + Jsoup 1.17
- **数据库**：H2（开发）/ SQLite（生产）
- **构建**：Gradle

### 前端
- **框架**：Electron 28 + Vue 3.4
- **语言**：TypeScript 5.3
- **UI库**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **构建**：Vite

### 通信
- **REST API**：前后端数据交互
- **WebSocket**：实时日志、TTS进度同步

## 开发环境

### 前置要求
- JDK 17 (Eclipse Temurin)
- Node.js 18+
- Git

### 快速开始

# 克隆仓库
git clone https://github.com/你的用户名/moyue-reader.git
cd moyue-reader

# 启动后端服务
cd backend
./gradlew bootRun

# 新终端启动前端应用
cd frontend
npm install
npm run electron:dev

构建

# 后端打包
cd backend
./gradlew bootJar

# 前端打包（含 JRE）
cd frontend
npm run electron:build

下载安装

访问 Releases 页面下载对应平台的安装包：

· Windows：.exe 安装包
· macOS：.dmg 安装包
· Linux：.AppImage 或 .deb 包

贡献指南

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
3. 提交更改 (git commit -m 'Add some AmazingFeature')
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
