pluginManagement {
    repositories {
        // 阿里云镜像 - 插件仓库
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        
        // 备用：原始仓库
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 配置 JDK 工具链下载
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 阿里云镜像 - 依赖仓库
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        
        // 备用：原始仓库
        google()
        mavenCentral()
    }
}

rootProject.name = "VoiceLifeAssistant"
include(":app")
