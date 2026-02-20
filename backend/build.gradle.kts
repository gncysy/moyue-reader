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
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Database
    implementation("com.h2database:h2")                // 开发
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")   // 生产
    
    // 书源引擎
    implementation("org.mozilla:rhino:1.7.15")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ✅ 缓存（新增）
    implementation("com.github.ben-manes:caffeine:caffeine:3.1.8")
    
    // 工具
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("commons-codec:commons-codec:1.16.0")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("moyue-backend.jar")
}

tasks.register<Zip>("jlinkZip") {
    dependsOn("bootJar")
    group = "build"
    description = "使用 jlink 创建自定义 JRE 并打包成 zip"
    
    val jreDir = file("$buildDir/custom-jre")
    doFirst {
        delete(jreDir)
        exec {
            commandLine(
                "jlink",
                "--module-path", System.getProperty("java.home") + "/jmods",
                "--add-modules", "java.base,java.sql,java.naming,java.management,java.xml,java.logging,java.desktop",
                "--output", jreDir.absolutePath,
                "--strip-debug",
                "--compress", "2",
                "--no-header-files",
                "--no-man-pages"
            )
        }
    }
    from(jreDir)
    from("build/libs") {
        include("moyue-backend.jar")
    }
    archiveFileName.set("moyue-jre-${version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}
