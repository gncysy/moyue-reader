package com.moyue
 
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
 
/**
 * Moyue Reader 后端主应用类
 *
 * Spring Boot 4.0.3 + Kotlin 2.3.10
 * 集成：JPA、WebSocket、Security、Actuator、缓存
 *
 * @author Moyue Team
 * @since 4.0.3
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = ["com.moyue"])
class MoyueApplication
 
fun main(args: Array<String>) {
    runApplication<MoyueApplication>(*args)
}
