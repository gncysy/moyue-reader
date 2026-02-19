# 墨阅 (Moyue)

<div align="center">
  <p><strong>将 Legado 书源生态带到桌面的跨平台阅读器</strong></p>
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
  <p>
    English | 简体中文
  </p>
</div>

## 项目简介

墨阅是一个跨平台桌面阅读器，将 Android 开源阅读器 Legado 的核心功能重构为桌面应用。项目采用前后端分离架构，后端基于 Spring Boot + Kotlin，前端基于 Electron + Vue 3。

目标是在保持 Legado 书源生态兼容性的同时，提供更精致的桌面端阅读体验。

## 截图预览

> 截图准备中，敬请期待

| 书架 | 阅读器 | 书源管理 |
|------|--------|----------|
| ![书架](https://via.placeholder.com/300x200?text=书架预览) | ![阅读器](https://via.placeholder.com/300x200?text=阅读器预览) | ![书源](https://via.placeholder.com/300x200?text=书源预览) |

## 功能特性

### 书架管理
- 书籍增删改查
- 阅读进度记录
- 封面缓存
- 分组管理

### 书源系统
- 导入/导出 Legado 格式书源（JSON）
- 书源启用/禁用
- 书源测试
- 兼容性分析

### 阅读器
- 四种翻页模式：覆盖、仿真、滑动、滚动
- 排版设置：字体、字号、行距
- 主题切换：日间、夜间、护眼
- 目录导航
- 书签

### 安全沙箱
| 模式 | 兼容性 | 限制 |
| --- | --- | --- |
| 标准模式 | ~85% | 禁止文件/Socket/反射 |
| 兼容模式 | ~95% | 允许文件/Socket，禁止反射 |
| 信任模式 | ~98% | 仅超时保护，需手动确认 |

### TTS 朗读
- 系统 TTS（Windows SAPI5 / macOS AVSpeech）
- 语速调节
- 后台播放

### WebDAV 同步
- 书架备份与恢复
- 阅读进度同步
- 手动/自动同步

### 本地书籍
- TXT：自动分章
- EPUB：解析目录、封面、正文

## 系统架构

┌─────────────────────────────────────┐
│         Electron 前端层              │
│  ┌─────────┐ ┌─────────┐           │
│  │  书架   │ │ 阅读器  │           │
│  └─────────┘ └─────────┘           │
├─────────────────────────────────────┤
│         HTTP / WebSocket             │
├─────────────────────────────────────┤
│         Spring Boot 后端层           │
│  ┌─────────┐ ┌─────────┐           │
│  │ 书源引擎 │ │ 安全沙箱 │           │
│  └─────────┘ └─────────┘           │
├─────────────────────────────────────┤
│         Rhino 1.7.15 引擎           │
│  ┌─────────┐ ┌─────────┐           │
│  │ Jsoup   │ │ OkHttp  │           │
│  └─────────┘ └─────────┘           │
└─────────────────────────────────────┘

## 技术栈

### 后端
- Kotlin 1.9 + Spring Boot 3.2
- Rhino 1.7.15（书源引擎）
- OkHttp + Jsoup
- H2 / SQLite

### 前端
- Electron 28 + Vue 3.4
- TypeScript 5.3
- Element Plus
- Pinia + Vue Router

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

### 启动后端
```bash
cd backend
./gradlew bootRun
```

### 启动前端
新开终端窗口，执行：
```bash
cd frontend
npm install
npm run electron:dev
```

## 下载安装

访问 https://github.com/gncysy/moyue-reader/releases 页面下载对应平台安装包：

- Windows：.exe
- macOS：.dmg
- Linux：.AppImage / .deb

## 贡献指南

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (git checkout -b feature/AmazingFeature)
3. 提交更改 (git commit -m 'feat: add feature')
4. 推送到分支 (git push origin feature/AmazingFeature)
5. 打开 Pull Request

## 致谢

感谢以下开源项目的支持：

- [Legado](https://github.com/gedoor/legado) - Android 开源阅读器，书源生态的核心
- [Reader](https://github.com/hectorqin/reader) - 阅读3服务器版
- [Rhino](https://github.com/mozilla/rhino) - JavaScript 引擎
- [Electron](https://www.electronjs.org/) - 跨平台桌面应用框架
- [Spring Boot](https://spring.io/projects/spring-boot) - Java 后端框架
- [Vue.js](https://vuejs.org/) - 前端框架

## 贡献者

<a href="https://github.com/gncysy">
  <img src="https://avatars.githubusercontent.com/u/你的ID?v=4" width="50" height="50" style="border-radius:50%" alt="gncysy"/>
</a>

## 许可证

GPL-3.0 License © 2024 墨阅团队

## 联系方式

- 问题反馈：https://github.com/gncysy/moyue-reader/issues

✅ 完整英文版（与中文版完全对应）

## English

<div align="center">
  <p><strong>A cross-platform desktop reader that brings the Legado book source ecosystem to your desktop</strong></p>
</div>

### Introduction

Moyue is a cross-platform desktop reader that reimagines the core functionality of the Android open-source reader Legado for desktop environments. It features a frontend-backend separation architecture, with a backend built on Spring Boot + Kotlin and a frontend powered by Electron + Vue 3.

The goal is to deliver a refined desktop reading experience while maintaining full compatibility with the Legado book source ecosystem.

### Screenshots

> Screenshots coming soon

| Bookshelf | Reader | Book Sources |
|-----------|--------|--------------|
| ![Bookshelf](https://via.placeholder.com/300x200?text=Bookshelf+Preview) | ![Reader](https://via.placeholder.com/300x200?text=Reader+Preview) | ![Book Sources](https://via.placeholder.com/300x200?text=Sources+Preview) |

### Features

#### 📚 Bookshelf Management
- Add, delete, and modify books
- Reading progress tracking
- Cover caching
- Custom grouping

#### 🔍 Book Source System
- Import/export Legado format book sources (JSON)
- Enable/disable book sources
- Book source testing
- Compatibility analysis

#### 📖 Reader
- Four page-turning modes: cover, simulation, slide, scroll
- Typography settings: font, size, line spacing
- Theme switching: day, night, eye-protection
- Table of contents navigation
- Bookmarks

#### 🛡️ Security Sandbox
| Mode | Compatibility | Restrictions |
| --- | --- | --- |
| Standard | ~85% | No file/Socket/reflection |
| Compatible | ~95% | File/Socket allowed, no reflection |
| Trusted | ~98% | Timeout protection only, manual confirmation required |

#### 🔊 TTS Reading
- System TTS (Windows SAPI5 / macOS AVSpeech)
- Speed adjustment
- Background playback

#### 🔄 WebDAV Sync
- Bookshelf backup and restore
- Reading progress sync
- Manual/auto sync modes

#### 📄 Local Books
- TXT: automatic chapter detection
- EPUB: parse table of contents, cover, content

### System Architecture

┌─────────────────────────────────────┐
│         Electron Frontend            │
│  ┌─────────┐ ┌─────────┐           │
│  │ Bookshelf│ │ Reader  │           │
│  └─────────┘ └─────────┘           │
├─────────────────────────────────────┤
│         HTTP / WebSocket             │
├─────────────────────────────────────┤
│         Spring Boot Backend          │
│  ┌─────────┐ ┌─────────┐           │
│  │Source   │ │Security │           │
│  │Engine   │ │Sandbox  │           │
│  └─────────┘ └─────────┘           │
├─────────────────────────────────────┤
│         Rhino 1.7.15 Engine         │
│  ┌─────────┐ ┌─────────┐           │
│  │ Jsoup   │ │ OkHttp  │           │
│  └─────────┘ └─────────┘           │
└─────────────────────────────────────┘

### Tech Stack

#### Backend
- Kotlin 1.9 + Spring Boot 3.2
- Rhino 1.7.15 (book source engine)
- OkHttp + Jsoup
- H2 / SQLite

#### Frontend
- Electron 28 + Vue 3.4
- TypeScript 5.3
- Element Plus
- Pinia + Vue Router

### Quick Start

#### Requirements
- JDK 17 (Eclipse Temurin)
- Node.js 18+
- Git

#### Clone
```bash
git clone https://github.com/gncysy/moyue-reader.git
cd moyue-reader
```

#### Start Backend
```bash
cd backend
./gradlew bootRun
```

#### Start Frontend
Open a new terminal window:
```bash
cd frontend
npm install
npm run electron:dev
```

### Download

Visit the Releases page to download platform-specific packages:

- Windows: .exe
- macOS: .dmg
- Linux: .AppImage / .deb

### Acknowledgements

Thanks to the following open-source projects:

- Legado - Android open-source reader, the core of the book source ecosystem
- Reader - Reader 3 server edition
- Rhino - JavaScript engine
- Electron - Cross-platform desktop framework
- Spring Boot - Java backend framework
- Vue.js - Frontend framework

### License

GPL-3.0 License © 2024 Moyue Team

### Contact

- Issues: https://github.com/gncysy/moyue-reader/issues
