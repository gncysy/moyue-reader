import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("io.ktor.plugin") version "3.1.2"
}
 
group = "com.moyue"
version = "0.1.0"
 
application {
    mainClass.set("com.moyue.ApplicationKt")
}
 
java {
    sourceCompatibility = JavaVersion.VERSION_17
}
 
repositories {
    mavenCentral()
}
 
dependencies {
    // ==================== Ktor Server 核心 ====================
    implementation("io.ktor:ktor-server-core-jvm:3.1.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.1.2")
    implementation("io.ktor:ktor-server-websockets-jvm:3.1.2")
    
    // ==================== Ktor 扩展功能 ====================
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.2")
    implementation("io.ktor:ktor-server-cors-jvm:3.1.2")
    implementation("io.ktor:ktor-server-compression-jvm:3.1.2")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.1.2")
    
    // ==================== Ktor Client（书源内部使用） ====================
    implementation("io.ktor:ktor-client-core-jvm:3.1.2")
    implementation("io.ktor:ktor-client-cio-jvm:3.1.2")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:3.1.2")
    
    // ==================== 依赖注入 ====================
    implementation("io.insert-koin:koin-ktor:4.0.4")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.4")
    
    // ==================== 数据库与 ORM ====================
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("com.zaxxer:HikariCP:6.3.0")
    
    // 数据库驱动
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")
    runtimeOnly("com.h2database:h2:2.4.240") // 仅测试用
    
    // ==================== Kotlin 基础 ====================
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
    
    // ==================== 日志 ====================
    implementation("ch.qos.logback:logback-classic:1.5.18")
    
    // ==================== JavaScript 引擎 ====================
    implementation("org.mozilla:rhino:1.7.15")
    
    // ==================== 网络与解析 ====================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.0")
    
    // ==================== 工具类 ====================
    implementation("commons-codec:commons-codec:1.18.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // ==================== 测试 ====================
    testImplementation("io.ktor:ktor-server-tests-jvm:3.1.2")
    testImplementation("io.ktor:ktor-client-mock:3.1.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.insert-koin:koin-test:4.0.4")
    testImplementation("io.insert-koin:koin-test-junit5:4.0.4")
}
 
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        freeCompilerArgs.add("-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
    }
}
 
tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("ktor.environment", "test")
}
 
// Ktor 运行配置
tasks.named<JavaExec>("run") {
    jvmArgs = listOf(
        "-server",
        "-Xms256m",
        "-Xmx1024m",
        "-XX:+UseG1GC",
        "-XX:+UseStringDeduplication",
        "-XX:MaxGCPauseMillis=100",
        "-XX:InitiatingHeapOccupancyPercent=45",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=${layout.buildDirectory.get()}/heap-dump.hprof",
        "-Djava.awt.headless=true",
        "-Dktor.environment=prod",
        "-Dlogging.level.root=INFO",
        "-Dlogging.level.com.moyue=DEBUG"
    )
}
 
// Ktor 打包为可执行 JAR
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Add-Opens" to "java.base/java.lang java.base/java.lang.reflect java.base/java.util java.base/java.text"
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
    archiveFileName.set("moyue-backend.jar")
}
