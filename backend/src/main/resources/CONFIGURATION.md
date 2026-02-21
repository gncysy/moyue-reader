# 墨阅后端配置说明

## 环境配置

### 启动方式

开发环境：
java -jar moyue-backend.jar --spring.profiles.active=dev

生产环境：
java -jar moyue-backend.jar --spring.profiles.active=prod

自定义环境：
java -jar moyue-backend.jar --spring.profiles.active=custom

### 环境变量

变量名                    | 默认值               | 说明
-------------------------|----------------------|---------------------------
SERVER_PORT               | 0                    | 服务端口（0表示随机）
SERVER_ADDRESS            | 127.0.0.1            | 监听地址
MOYUE_DATA_HOME           | ~/MoyueData          | 数据目录
MOYUE_SECURITY_LEVEL      | standard             | 安全级别（standard/compatible/trusted）
DB_USERNAME               | sa                   | 数据库用户名
DB_PASSWORD               | (空)                 | 数据库密码
DDL_AUTO                  | update               | JPA DDL 策略（update/validate/none）
H2_CONSOLE_ENABLED        | false                | 是否启用 H2 控制台
SHOW_SQL                  | false                | 是否打印 SQL 语句
LOG_LEVEL_ROOT            | INFO                 | 根日志级别
LOG_LEVEL_APP             | INFO                 | 应用日志级别
LOG_LEVEL_SPRING          | WARN                 | Spring 框架日志级别
LOG_LEVEL_HIBERNATE       | WARN                 | Hibernate 日志级别

### 配置文件说明

文件名                          | 用途                           | 优先级
-------------------------------|--------------------------------|--------
application.yml                | 基础配置，所有环境共享         | 1
application-dev.yml             | 开发环境配置，覆盖基础配置     | 2
application-prod.yml           | 生产环境配置，覆盖基础配置     | 2

配置加载顺序：application.yml -> application-{profile}.yml -> 环境变量（优先级最高）

## 安全建议

### 生产环境必须设置

export DB_PASSWORD="你的强密码"
export DDL_AUTO=validate
export H2_CONSOLE_ENABLED=false
export MOYUE_SECURITY_LEVEL=standard
export LOG_LEVEL_APP=INFO

### 禁止事项

1. 生产环境不要启用 H2 控制台
2. 不要使用 ddl-auto: create-drop
3. 不要将日志级别设为 DEBUG
4. 不要使用空密码

### 数据备份

- 定期备份 MOYUE_DATA_HOME 目录
- 建议使用 ddl-auto: validate 防止意外数据丢失
- 备份目录通常在：
  - Windows: C:\Users\{用户名}\MoyueData
  - Linux: /var/lib/moyue

## 日志配置

### 日志文件位置

默认位置：{MOYUE_DATA_HOME}/logs/moyue-backend.log

### 日志策略

- 单个文件最大 50MB
- 保留 30 天历史
- 总大小上限 1GB
- 自动滚动，无需手动清理

### 日志级别说明

级别  | 用途
------|--------------------------
TRACE | 最详细，包含所有调用信息
DEBUG | 调试信息，开发环境使用
INFO  | 一般信息，生产环境推荐
WARN  | 警告信息，不影响运行
ERROR | 错误信息，需要处理

### 查看日志

实时查看：
tail -f {MOYUE_DATA_HOME}/logs/moyue-backend.log

查看错误：
grep ERROR {MOYUE_DATA_HOME}/logs/moyue-backend.log

## 性能配置

### 缓存配置（moyue.performance.cache）

配置项        | 默认值 | 说明
-------------|--------|---------------------
enabled      | true   | 是否启用缓存
ttl          | 3600   | 缓存过期时间（秒）
max-size     | 1000   | 最大缓存数量

### 书源配置（moyue.book-source）

配置项               | 默认值 | 说明
--------------------|--------|---------------------
max-concurrent-search | 5      | 最大并发搜索数
search-timeout       | 30     | 搜索超时时间（秒）
request-timeout      | 10     | 请求超时时间（秒）

## 管理端点

### 可用端点

- /actuator/health - 健康检查
- /actuator/info - 应用信息

### 访问控制

生产环境建议通过防火墙限制访问，或配置 Spring Security。

## 故障排查

### 服务启动失败

1. 检查日志文件：{MOYUE_DATA_HOME}/logs/moyue-backend.log
2. 检查端口占用：netstat -ano | findstr {端口号}（Windows）
3. 检查数据目录权限

### 数据库连接失败

1. 检查 H2 数据库文件是否存在
2. 检查数据库配置是否正确
3. 检查是否有其他进程占用数据库文件

### 性能问题

1. 开发环境禁用缓存：设置 moyue.performance.cache.enabled=false
2. 调整连接池大小：修改 spring.datasource.hikari.maximum-pool-size
3. 查看慢查询日志

## 升级说明

### 升级前

1. 备份数据目录 MOYUE_DATA_HOME
2. 备份当前配置文件
3. 停止服务

### 升级后

1. 检查配置项是否需要调整
2. 启动服务，查看日志
3. 验证功能是否正常
