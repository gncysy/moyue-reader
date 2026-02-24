-- ============================================
-- Moyue Reader - 数据库初始化脚本
-- Spring Boot 4.0.3 + Kotlin 2.3.10
-- ============================================
 
-- 启用外键约束（SQLite）
PRAGMA foreign_keys = ON;
 
-- ==================== 书源表 ====================
CREATE TABLE IF NOT EXISTS book_sources (
    id VARCHAR(36) PRIMARY KEY,
    source_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    icon VARCHAR(1000),
    url VARCHAR(1000),
    author VARCHAR(200),
    enabled BOOLEAN NOT NULL DEFAULT 1,
    weight INTEGER NOT NULL DEFAULT 0,
    book_source_rule_id VARCHAR(100),
    last_used_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    check_status VARCHAR(20),
    check_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE INDEX IF NOT EXISTS idx_source_id ON book_sources(source_id);
CREATE INDEX IF NOT EXISTS idx_enabled_weight ON book_sources(enabled, weight DESC);
CREATE INDEX IF NOT EXISTS idx_last_used_at ON book_sources(last_used_at);
 
-- ==================== 书源规则表 ====================
CREATE TABLE IF NOT EXISTS book_source_rules (
    id VARCHAR(36) PRIMARY KEY,
    rule_id VARCHAR(100) UNIQUE NOT NULL,
    search_url TEXT,
    search_list TEXT,
    search_name TEXT,
    search_author TEXT,
    search_cover_url TEXT,
    search_book_url TEXT,
    book_url TEXT,
    book_info TEXT,
    chapter_list TEXT,
    chapter_name TEXT,
    chapter_url TEXT,
    content_url TEXT,
    content TEXT,
    headers TEXT,
    charset VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE INDEX IF NOT EXISTS idx_rule_id ON book_source_rules(rule_id);
 
-- ==================== 书籍表 ====================
CREATE TABLE IF NOT EXISTS books (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    author VARCHAR(200) NOT NULL,
    cover_url VARCHAR(1000),
    intro TEXT,
    book_url VARCHAR(2000) NOT NULL,
    origin VARCHAR(100),
    source_id VARCHAR(36),
    chapter_count INTEGER NOT NULL DEFAULT 0,
    current_chapter INTEGER NOT NULL DEFAULT 0,
    progress INTEGER NOT NULL DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES book_sources(id) ON DELETE SET NULL
);
 
CREATE INDEX IF NOT EXISTS idx_origin ON books(origin);
CREATE INDEX IF NOT EXISTS idx_last_read_at ON books(last_read_at);
CREATE INDEX IF NOT EXISTS idx_updated_at ON books(updated_at);
 
-- ==================== 章节表 ====================
CREATE TABLE IF NOT EXISTS book_chapters (
    id VARCHAR(36) PRIMARY KEY,
    book_id VARCHAR(36) NOT NULL,
    chapter_index INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    is_vip BOOLEAN NOT NULL DEFAULT 0,
    content TEXT,
    content_cached_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);
 
CREATE INDEX IF NOT EXISTS idx_book_id ON book_chapters(book_id);
CREATE INDEX IF NOT EXISTS idx_book_index ON book_chapters(book_id, chapter_index);
 
-- ==================== 插入初始数据 ====================
-- 插入默认书源规则（示例）
INSERT OR IGNORE INTO book_source_rules (
    id, rule_id, enabled
) VALUES (
    'default-rule-id',
    'default-source',
    1
);
 
-- 插入默认书源（示例）
INSERT OR IGNORE INTO book_sources (
    id, source_id, name, enabled, weight, book_source_rule_id
) VALUES (
    'default-source-id',
    'default-source',
    '默认书源',
    1,
    0,
    'default-rule-id'
);
