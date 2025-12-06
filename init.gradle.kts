// Gradle初始化脚本 - 强制使用国内镜像
// 将此文件复制到 %USERPROFILE%\.gradle\init.gradle.kts

gradle.projectsLoaded {
    rootProject.allprojects {
        buildscript {
            repositories {
                all {
                    if (this is MavenArtifactRepository) {
                        val url = this.url.toString()
                        if (url.contains("repo1.maven.org") || 
                            url.contains("repo.maven.apache.org") ||
                            url.contains("jcenter.bintray.com") ||
                            url.contains("dl.google.com") ||
                            url.contains("maven.google.com")) {
                            println("替换仓库: $url -> 阿里云镜像")
                        }
                    }
                }
                
                // 清空并重新添加阿里云镜像
                clear()
                maven { url = uri("https://maven.aliyun.com/repository/google") }
                maven { url = uri("https://maven.aliyun.com/repository/public") }
                maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
                maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            }
        }
        
        repositories {
            all {
                if (this is MavenArtifactRepository) {
                    val url = this.url.toString()
                    if (url.contains("repo1.maven.org") || 
                        url.contains("repo.maven.apache.org") ||
                        url.contains("jcenter.bintray.com") ||
                        url.contains("dl.google.com") ||
                        url.contains("maven.google.com")) {
                        println("替换仓库: $url -> 阿里云镜像")
                    }
                }
            }
            
            // 清空并重新添加阿里云镜像
            clear()
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        }
    }
}
