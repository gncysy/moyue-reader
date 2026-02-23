rootProject.name = "moyue-backend"
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（国内首选）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        
        // 腾讯云镜像（备选）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 华为云镜像（备选）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 官方源（保底）
        mavenCentral()
        google()
    }
}
