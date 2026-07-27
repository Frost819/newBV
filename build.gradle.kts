// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "jacoco")

    // detekt 配置：使用根目录的 config/detekt.yml
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        config.setFrom(rootProject.files("config/detekt.yml"))
        buildUponDefaultConfig = true
    }

    // JaCoCo：所有模块统一生成覆盖率报告
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }
}

apply(plugin = "jacoco")

// 聚合所有子模块的 JaCoCo 报告到根项目
tasks.register<JacocoReport>("jacocoAggregatedReport") {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report for all modules."

    sourceDirectories.setFrom(files(subprojects.mapNotNull { sub ->
        sub.extensions.findByType<org.gradle.api.plugins.JavaPluginExtension>()
            ?.sourceSets?.findByName("main")?.allSource?.srcDirs
    }))

    executionData.setFrom(fileTree(rootDir) {
        include("**/build/jacoco/*.exec")
        include("**/build/outputs/unit_test_code_coverage/**/*.exec")
        include("**/build/outputs/code_coverage/**/*.ec")
    })
}
