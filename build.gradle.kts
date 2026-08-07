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

    // Android library 模块：创建 jacocoTestReport task
    plugins.withId("com.android.library") {
        val createDebugJacocoReport by tasks.registering(JacocoReport::class) {
            group = "verification"
            description = "Generates JaCoCo coverage report for debug unit tests."
            dependsOn("testDebugUnitTest")
            sourceDirectories.setFrom(
                files("src/main/kotlin", "src/main/java"),
            )
            classDirectories.setFrom(
                fileTree("${layout.buildDirectory.get().asFile}/intermediates/built_in_kotlinc/debug") {
                    include("**/*.class")
                    exclude("**/*_Hilt*.class")
                    exclude("**/Hilt_*.class")
                    exclude("**/Dagger*.class")
                    exclude("**/*_Factory.class")
                    exclude("**/*_MembersInjector.class")
                },
                fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
                    include("**/*.class")
                },
            )
            executionData.setFrom(
                fileTree(layout.buildDirectory) {
                    include("outputs/unit_test_code_coverage/**/*.exec")
                    include("jacoco/*.exec")
                }
            )
        }
    }

    // Android application 模块：创建 jacocoTestReport task
    plugins.withId("com.android.application") {
        val createDebugJacocoReport by tasks.registering(JacocoReport::class) {
            group = "verification"
            description = "Generates JaCoCo coverage report for debug unit tests."
            dependsOn("testDebugUnitTest")
            sourceDirectories.setFrom(
                files("src/main/kotlin", "src/main/java"),
            )
            classDirectories.setFrom(
                fileTree("${layout.buildDirectory.get().asFile}/intermediates/built_in_kotlinc/debug") {
                    include("**/*.class")
                    exclude("**/*_Hilt*.class")
                    exclude("**/Hilt_*.class")
                    exclude("**/Dagger*.class")
                    exclude("**/*_Factory.class")
                    exclude("**/*_MembersInjector.class")
                },
                fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug") {
                    include("**/*.class")
                },
            )
            executionData.setFrom(
                fileTree(layout.buildDirectory) {
                    include("outputs/unit_test_code_coverage/**/*.exec")
                    include("jacoco/*.exec")
                }
            )
        }
    }
}

apply(plugin = "jacoco")

// 聚合所有子模块的 JaCoCo 报告到根项目
val jacocoAggregatedReport = tasks.register<JacocoReport>("jacocoAggregatedReport") {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report for all modules."

    sourceDirectories.setFrom(files(subprojects.mapNotNull { sub ->
        sub.extensions.findByType<org.gradle.api.plugins.JavaPluginExtension>()
            ?.sourceSets?.findByName("main")?.allSource?.srcDirs
    }))

    // 从各模块 build 目录收集编译后的 class 文件
    // JVM 模块: build/classes/kotlin/main
    // Android 模块: build/intermediates/javac/debug + built_in_kotlinc
    classDirectories.setFrom(
        fileTree(rootDir) {
            include("**/build/classes/kotlin/main/**/*.class")
            include("**/build/intermediates/javac/debug/classes/**/*.class")
            include("**/build/intermediates/built_in_kotlinc/debug/**/*.class")
            exclude("**/buildSrc/**")
            exclude("**/bv/**")
            exclude("**/bili-api-grpc/**")
            exclude("**/*_Hilt*.class")
            exclude("**/Hilt_*.class")
            exclude("**/Dagger*.class")
            exclude("**/*_Factory.class")
            exclude("**/*_MembersInjector.class")
            exclude("**/*_Generated*.class")
            exclude("**/dagger/**")
            exclude("**/hilt_aggregated_deps/**")
            exclude("**/bilibili/**")
        }
    )

    executionData.setFrom(fileTree(rootDir) {
        include("**/build/jacoco/*.exec")
        include("**/build/outputs/unit_test_code_coverage/**/*.exec")
        include("**/build/outputs/code_coverage/**/*.ec")
    })
}

// 确保聚合报告在所有测试任务完成后运行
subprojects {
    tasks.matching { it.name in listOf("test", "testDebugUnitTest") }
        .configureEach {
            jacocoAggregatedReport.get().mustRunAfter(this)
        }
}
