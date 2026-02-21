# OkHttp 配置
okhttp:
  connect-timeout: 10  # 连接超时（秒）
  read-timeout: 30    # 读取超时（秒）
  write-timeout: 10   # 写入超时（秒）
  connection-pool:
    max-idle: 10       # 最大空闲连接数
    keep-alive: 5      # 保持活跃时间（分钟）
  logging:
    enabled: true      # 是否启用日志（生产环境建议 false）
    level: BASIC       # 日志级别：NONE/BASIC/HEADERS/BODY
  proxy:
    enabled: false     # 是否启用代理
    host: ""           # 代理主机
    port: 8080         # 代理端口

# 异步任务配置
async:
  core-pool-size: 5   # 核心线程数
  max-pool-size: 10   # 最大线程数
  queue-capacity: 100 # 队列容量
