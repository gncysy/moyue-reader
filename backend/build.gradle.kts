import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
 
plugins {
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
    kotlin("plugin.jpa") version "2.3.10"
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
    // ============ Spring Boot 核心 ============
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // ============ Kotlin（让 Spring Boot 4.0.3 管理） ============
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("tools.jackson.datatype:jackson-datatype-jsr310")
    
    // ============ 数据库 ============
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    runtimeOnly("com.h2database:h2")
    
    // ============ JavaScript 引擎 ============
    implementation("org.mozilla:rhino:1.7.15")
    
    // ============ 网络与解析 ============
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("com.google.code.gson:gson:2.13.2")
    
    // ============ 加密与工具 ============
    implementation("commons-codec:commons-codec:1.18.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.github.ben-manes.caffeine:caffeine")
    
    // ============ 开发工具 ============
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // ============ 测试 ============
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.springframework.security:spring-security-test")
}
 
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xjspecify-strict"
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
 
// CDS 优化任务
tasks.register<Exec>("prepareCDS") {
    group = "build"
    description = "准备 CDS 共享类数据"
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
 
// jlink 打包
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
