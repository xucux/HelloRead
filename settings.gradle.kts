pluginManagement {
    repositories {
        // 阿里云镜像仓库 - 国内访问速度更快
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central/")

        // 华为云镜像仓库 - 备用选择
        maven("https://mirrors.huaweicloud.com/repository/maven")
        // 专门用于 JetBrains 产品的仓库
        maven("https://maven.aliyun.com/repository/jetbrains-intellij-releases")
        // 或者使用更通用的 JB 仓库（包含 Kotlin 等）
        maven("https://maven.aliyun.com/repository/jetbrains-public")

        maven("https://repo.huaweicloud.com/repository/jetbrains-intellij-releases")
        maven("https://repo.huaweicloud.com/repository/jetbrains-public")
        // 原始仓库作为备用
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

rootProject.name = "HelloRead"