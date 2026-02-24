# ============================================================================
# Moyue Reader 后端主配置文件
# Spring Boot 4.0.3
# ============================================================================
 
spring:
  application:
    name: moyue-backend
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  # ==================== 数据源配置 ====================
  datasource:
    url: jdbc:sqlite:${moyue.data.home:~/MoyueData}/moyue.db
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  # ==================== JPA 配置 ====================
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: ${SHOW_SQL:false}
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.community.dialect.SQLiteDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 50
          order_inserts: true
          order_updates: true
  
  # ==================== Jackson 配置 ====================
  jackson:
    json:
      read:
        date-format: yyyy-MM-dd HH:mm:ss
        time-zone: GMT+8
      write:
        date-format: yyyy-MM-dd HH:mm:ss
        time-zone: GMT+8
    serialization:
      write-dates-as-timestamps: false
      indent-output: false
    deserialization:
      fail-on-unknown-properties: false
  
  # ==================== 缓存配置 ====================
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=3600s
  
  # ==================== Devtools 配置 ====================
  devtools:
    livereload:
      enabled: ${LIVERELOAD_ENABLED:true}
    restart:
      enabled: ${RESTART_ENABLED:true}
  
  # ==================== Servlet 编码配置 ====================
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
  
  # ==================== 持久化配置 ====================
  persistence:
    exceptiontranslation:
      enabled: true
 
# ==================== Web 错误配置 ====================
spring:
  web:
    error:
      include-message: always
      include-binding-errors: always
      include-stacktrace: on-param
      include-exception: false
 
# ==================== 安全配置 ====================
spring:
  security:
    user:
      name: ${SECURITY_USER:admin}
      password: ${SECURITY_PASSWORD:admin123}
    filter:
      order: 10
 
# ==================== 服务器配置 ====================
server:
  port: ${SERVER_PORT:8080}
  address: ${SERVER_ADDRESS:127.0.0.1}
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 10
      accept-count: 100
    connection-timeout: 20000
    max-connections: 8192
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
 
# ==================== 日志配置 ====================
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.moyue: ${LOG_LEVEL_APP:INFO}
    org.springframework: ${LOG_LEVEL_SPRING:WARN}
    org.hibernate: ${LOG_LEVEL_HIBERNATE:WARN}
    tools.jackson: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: ${moyue.data.home:~/MoyueData}/logs/moyue-backend.log
    max-size: 50MB
    max-history: 30
    total-size-cap: 1GB
 
# ==================== Actuator 配置 ====================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: false  # 默认不启用探针
  metrics:
    export:
      simple:
        enabled: true
 
# ==================== 应用自定义配置 ====================
moyue:
  data:
    home: ${MOYUE_DATA_HOME:~/MoyueData}
  
  security:
    default-level: ${MOYUE_SECURITY_LEVEL:standard}  # standard | compatible | trusted
    jwt:
      secret: ${JWT_SECRET:moyue-secret-key-change-in-production}
      expiration: ${JWT_EXPIRATION:86400}  # 秒
  
  performance:
    cache:
      enabled: true
      ttl: 3600
      max-size: 1000
  
  book-source:
    max-concurrent-search: 5
    search-timeout: 30
    request-timeout: 10
    max-sources: 100
 
# ==================== OkHttp 配置 ====================
okhttp:
  connect-timeout: 10
  read-timeout: 30
  write-timeout: 10
  connection-pool:
    max-idle: 10
    keep-alive: 5
  logging:
    enabled: ${OKHTTP_LOG_ENABLED:false}
    level: ${OKHTTP_LOG_LEVEL:BASIC}
  proxy:
    enabled: ${PROXY_ENABLED:false}
    host: ${PROXY_HOST:}
    port: ${PROXY_PORT:8080}
 
# ==================== 异步任务配置 ====================
async:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 100
  thread-name-prefix: async-
