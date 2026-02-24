import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
 
plugins {
    id("org.springframework.boot") version "4.0.3"              // ✅ 3.2.0 → 4.0.3
    id("io.spring.dependency-management") version "1.1.7"       // ✅ 1.1.4 → 1.1.7
    kotlin("jvm") version "2.3.10"                            // ✅ 1.9.22 → 2.3.10
    kotlin("plugin.spring") version "2.3.10"                 // ✅ 1.9.22 → 2.3.10
    kotlin("plugin.jpa") version "2.3.10"                     // ✅ 1.9.22 → 2.3.10
}
 
group = "com.moyue"
version = "0.1.0"
 
java {
    sourceCompatibility = JavaVersion.VERSION_17              // ✅ 保持 17（LTS）
}
 
repositories {
    mavenCentral()
}
 
dependencies {
    // ==================== Spring Boot 核心（Spring Boot 管理版本）====================
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // ==================== Kotlin（Spring Boot 管理版本）====================
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")  // ✅ Jackson 3.x 由 Spring 管理
    
    // ==================== 数据库 ====================
    // ✅ SQLite JDBC - Spring Boot 不管理，手动指定版本
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")                    // 3.44.1.0 → 3.51.2.0
    
    // ✅ Hibernate Community Dialects - Spring Boot 不管理，手动指定版本
    implementation("org.hibernate.orm:hibernate-community-dialects:6.6.8.Final")  // 最新稳定版
    
    // ✅ H2 - Spring Boot 管理，删除版本号
    runtimeOnly("com.h2database:h2")
    
    // ==================== JavaScript 引擎 ====================
    // ✅ Rhino - Spring Boot 不管理，保持当前版本
    implementation("org.mozilla:rhino:1.7.15")                        // 1.9.1 → 1.7.15（最新稳定）
    
    // ==================== 网络与解析 ====================
    // ✅ OkHttp - Spring Boot 不管理，保持当前稳定版本
    implementation("com.squareup.okhttp3:okhttp:4.12.0")                // 保持
    
    // ✅ Jsoup - Spring Boot 不管理，升级到最新稳定版
    implementation("org.jsoup:jsoup:1.22.1")                            // 1.17.2 → 1.22.1
    
    // ✅ Gson - Spring Boot 不管理，升级到最新稳定版
    implementation("com.google.code.gson:gson:2.13.2")                 // 2.10.1 → 2.13.2
    
    // ==================== 加密工具 ====================
    // ✅ Commons Codec - Spring Boot 不管理，升级到最新稳定版
    implementation("commons-codec:commons-codec:1.18.0")               // 1.16.1 → 1.18.0
    
    // ✅ Commons Lang3 - Spring Boot 不管理，添加最新稳定版
    implementation("org.apache.commons:commons-lang3:3.17.0")         // 新增
    
    // ==================== 缓存实现 ====================
    // ✅ Caffeine - Spring Boot 管理，删除版本号
    implementation("com.github.ben-manes.caffeine:caffeine")
    
    // ==================== 开发工具 ====================
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // ==================== 测试（Spring Boot 管理版本）====================
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
        jvmTarget = "17"                                             // ✅ 保持 17
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
        "-XX:HeapDumpPath=${buildDir}/heap-dump.hprof",
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
 
// 简化的 CDS 优化任务
tasks.register<Exec>("prepareCDS") {
    group = "build"
    description = "准备 CDS 共享类数据（需要先运行一次应用）"
    dependsOn("bootJar")
    
    val jarPath = "build/libs/moyue-backend.jar"
    val cdsPath = "build/libs/moyue.jsa"
    
    commandLine(
        "java",
        "-Xshare:dump",
        "-XX:SharedArchiveFile=$cdsPath",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:DumpLoadedClassList=${buildDir}/classes.lst",
        "-jar", jarPath
    )
}
 
// 改进的 jlink 打包
tasks.register<Zip>("jlinkZip") {
    dependsOn("bootJar")
    group = "build"
    description = "使用 jlink 创建自定义 JRE 并打包"
    
    val jreDir = file("$buildDir/custom-jre")
    val modules = listOf(
        "java.base",
        "java.sql",
        "java.naming",
        "java.management",
        "java.xml",
        "java.logging",
        "java.desktop",
        "java.security.jgss",
        "java.net.http",
        "jdk.httpserver",
        "jdk.unsupported"
    )
    
    doFirst {
        delete(jreDir)
        exec {
            commandLine(
                "jlink",
                "--module-path", "${System.getProperty("java.home")}/jmods",
                "--add-modules", modules.joinToString(","),
                "--output", jreDir.absolutePath,
                "--strip-debug",
                "--compress", "2",
                "--no-header-files",
                "--no-man-pages"
            )
        }
    }
    
    from(jreDir) {
        into("jre")
    }
    from("build/libs") {
        include("moyue-backend.jar")
        into("app")
    }
    
    archiveFileName.set("moyue-jre-${version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}
 
// 完整构建任务
tasks.register("fullBuild") {
    group = "build"
    description = "完整构建（jlink + 瘦身打包）"
    dependsOn("bootJar", "jlinkZip")
}
 
// 清理任务
tasks.register("cleanDist") {
    group = "build"
    description = "清理构建产物"
    doLast {
        delete("$buildDir/dist")
        delete("$buildDir/custom-jre")
        delete("$buildDir/heap-dump.hprof")
    }
}
