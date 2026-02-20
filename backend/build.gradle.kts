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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.h2database:h2")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // Rhino 升级到 1.9.1
    implementation("org.mozilla:rhino:1.9.1")
    
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.google.code.gson:gson:2.10.1")
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

tasks.bootRun {
    jvmArgs = listOf(
        "-server",
        "-Xms128m",
        "-Xmx512m",
        "-XX:+UseG1GC",
        "-XX:+UseStringDeduplication",
        "-XX:MaxGCPauseMillis=100",
        "-Djava.awt.headless=true"
    )
}

tasks.bootJar {
    archiveFileName.set("moyue-backend.jar")
    layered {
        enabled = true
    }
}

// CDS 优化：创建共享类数据
tasks.register<Exec>("createCDS") {
    group = "build"
    description = "创建 CDS 共享类数据，加速启动"
    dependsOn("bootJar")
    
    val jarPath = "build/libs/moyue-backend.jar"
    val cdsPath = "build/libs/moyue.jsa"
    
    onlyIf {
        !file(cdsPath).exists()
    }
    
    commandLine(
        "java",
        "-Xshare:dump",
        "-XX:SharedArchiveFile=$cdsPath",
        "-jar", jarPath
    )
}

// jlink 打包
tasks.register<Zip>("jlinkZip") {
    dependsOn("bootJar")
    group = "build"
    description = "使用 jlink 创建自定义 JRE 并打包"
    
    val jreDir = file("$buildDir/custom-jre")
    
    doFirst {
        delete(jreDir)
        exec {
            commandLine(
                "jlink",
                "--module-path", System.getProperty("java.home") + "/jmods",
                "--add-modules", "java.base,java.sql,java.naming,java.management,java.xml,java.logging,java.desktop,java.security.jgss",
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
        include("moyue.jsa")
    }
    
    archiveFileName.set("moyue-jre-${version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}

tasks.register("fullBuild") {
    group = "build"
    description = "完整构建，包含 CDS 优化"
    dependsOn("bootJar", "createCDS", "jlinkZip")
}
