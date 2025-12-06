pluginManagement {
    repositories {
        // CI环境使用原始仓库，本地使用阿里云镜像
        val isCI = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
        
        if (!isCI) {
            // 阿里云镜像 - 插件仓库（仅本地）
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        }
        
        // 原始仓库（CI和本地都使用）
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
        // CI环境使用原始仓库，本地使用阿里云镜像
        val isCI = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
        
        if (!isCI) {
            // 阿里云镜像 - 依赖仓库（仅本地）
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        }
        
        // 原始仓库（CI和本地都使用）
        google()
        mavenCentral()
    }
}

rootProject.name = "VoiceLifeAssistant"
include(":app")
