import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
 
plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}
 
group = "com.moyue"
version = "0.1.0"
 
java {
    sourceCompatibility = JavaVersion.VERSION_17
}
 
repositories {
    mavenCentral()
}
 
dependencies {
    // Spring Boot 核心
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // 数据库 - 使用 SQLite 作为主数据库，H2 仅用于测试
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.hibernate.orm:hibernate-community-dialects") // SQLite 方言支持
    runtimeOnly("com.h2database:h2") // 仅运行时依赖，用于测试
    
    // JavaScript 引擎 - Rhino 1.9.1 最新版本
    implementation("org.mozilla:rhino:1.9.1")
    
    // 网络与解析
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 加密工具
    implementation("commons-codec:commons-codec:1.16.1")
    
    // 缓存实现
    implementation("com.github.ben-manes.caffeine:caffeine")
    
    // 开发工具
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // 测试
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
 
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
        jvmTarget = "17"
        allWarningsAsErrors = false
    }
}
 
tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}
 
tasks.bootRun {
    jvmArgs = listOf(
        "-server",
        "-Xms256m",
        "-Xmx1024m",
        "-XX:+UseG1GC",
        "-XX:+UseStringDeduplication",
        "-XX:MaxGCPauseMillis=100",
        "-XX:InitiatingHeapOccupancyPercent=45",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Djava.awt.headless=true",
        "-Dspring.jpa.show-sql=false",
        "-Dlogging.level.root=INFO",
        "-Dlogging.level.com.moyue=DEBUG"
    )
}
 
tasks.bootJar {
    archiveFileName.set("moyue-backend.jar")
    layered {
        enabled = true
    }
    exclude("org/springframework/boot/devtools/**")
}
