/**
 * 应用全局配置。
 *
 * 集中管理包名、SDK 版本、版本号等构建配置，
 * 供各模块 build.gradle.kts 引用。
 */
object AppConfiguration {
    const val appId = "dev.frost819.newbv"
    const val applicationId = "dev.frost819.newbv"
    const val compileSdk = 36
    const val minSdk = 21
    const val targetSdk = 36

    private const val major = 0
    private const val minor = 1
    private const val patch = 0
    private const val hotFix = 0

    @Suppress("KotlinConstantConditions")
    val versionName: String
        get() {
            val base =
                System.getenv("NEWBV_VERSION_BASE")?.takeIf { it.isNotBlank() }
                    ?: "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}"
            return "$base.r$versionCode.${"git rev-list HEAD --abbrev-commit --max-count=1".exec()}"
        }
    val versionCode: Int
        get() = "git rev-list --count HEAD".exec().toIntOrNull() ?: 1
}

fun String.exec() = String(Runtime.getRuntime().exec(this).inputStream.readBytes()).trim()
