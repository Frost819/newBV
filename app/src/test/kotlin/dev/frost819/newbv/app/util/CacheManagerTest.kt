package dev.frost819.newbv.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * [CacheManager] 的单元测试。
 *
 * 验证缓存大小统计、LRU 清理策略、阈值触发与自动清空开关逻辑。
 * 使用 JUnit 5 临时目录，纯 JVM 运行，不依赖 Android 环境；
 * 阈值与开关通过构造参数注入，不依赖 Prefs 单例。
 */
class CacheManagerTest {

    @TempDir
    lateinit var tempDir: File

    private var thresholdMb = 200
    private var autoCleanEnabled = true

    @BeforeEach
    fun setUp() {
        thresholdMb = 200
        autoCleanEnabled = true
    }

    /** 构建被测对象，阈值与开关通过 lambda 注入（每次检查时读取，模拟实时变更）。 */
    private fun createManager(): CacheManager = CacheManager(
        cacheDirs = listOf(
            File(tempDir, "cache_a"),
            File(tempDir, "cache_b"),
        ),
        thresholdMb = { thresholdMb },
        autoCleanEnabled = { autoCleanEnabled },
    )

    /**
     * 在 [dir] 下创建指定大小的文件。
     *
     * @param sizeBytes 文件大小（字节）。
     * @param ageMs 文件修改时间回拨的毫秒数（值越大越"旧"，控制 LRU 顺序）。
     */
    private fun createFile(dir: File, name: String, sizeBytes: Int, ageMs: Long = 0L): File {
        val file = File(dir, name)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(sizeBytes))
        file.setLastModified(System.currentTimeMillis() - ageMs)
        return file
    }

    // ===== 大小统计 =====

    @Test
    fun `cacheSize returns 0 when dirs not exist`() {
        // Given: 未创建任何缓存文件
        val manager = createManager()

        // When
        val size = manager.cacheSize()

        // Then
        assertThat(size).isEqualTo(0L)
    }

    @Test
    fun `cacheSize sums files across all cache dirs recursively`() {
        // Given: 两个缓存目录多层结构共 450 字节
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val dirB = File(tempDir, "cache_b")
        createFile(dirA, "root.bin", 50)
        createFile(dirA, "sub/nested.bin", 100)
        createFile(dirB, "update.apk", 300)

        // When
        val size = manager.cacheSize()

        // Then
        assertThat(size).isEqualTo(450L)
    }

    // ===== LRU 清理策略 =====

    @Test
    fun `evictLru does nothing when under target`() {
        // Given: 两个目录共 200 字节，目标 1000 字节
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val dirB = File(tempDir, "cache_b")
        createFile(dirA, "a.bin", 100, ageMs = 10_000)
        createFile(dirB, "b.bin", 100, ageMs = 0)

        // When
        val freed = manager.evictLru(listOf(dirA, dirB), targetBytes = 1000L)

        // Then: 未超目标，无删除
        assertThat(freed).isEqualTo(0L)
        assertThat(manager.cacheSize()).isEqualTo(200L)
    }

    @Test
    fun `evictLru deletes oldest file across dirs first`() {
        // Given: 目录 A 的文件最旧，目录 B 的文件最新
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val dirB = File(tempDir, "cache_b")
        val oldest = createFile(dirA, "old.bin", 100, ageMs = 10_000)
        val newest = createFile(dirB, "new.bin", 100, ageMs = 0)

        // When: 目标 150 字节，需删除 50 字节以上
        val freed = manager.evictLru(listOf(dirA, dirB), targetBytes = 150L)

        // Then: 跨目录删除最旧的文件
        assertThat(freed).isEqualTo(100L)
        assertThat(oldest.exists()).isFalse()
        assertThat(newest.exists()).isTrue()
    }

    @Test
    fun `evictLru stops as soon as target reached`() {
        // Given: 三个 100 字节文件，总 300 字节
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val oldest = createFile(dirA, "oldest.bin", 100, ageMs = 3_000)
        val middle = createFile(dirA, "middle.bin", 100, ageMs = 2_000)
        val newest = createFile(dirA, "newest.bin", 100, ageMs = 1_000)

        // When: 目标 250 字节，删一个最旧文件即可满足
        val freed = manager.evictLru(listOf(dirA), targetBytes = 250L)

        // Then: 只删除最旧的一个
        assertThat(freed).isEqualTo(100L)
        assertThat(oldest.exists()).isFalse()
        assertThat(middle.exists()).isTrue()
        assertThat(newest.exists()).isTrue()
    }

    @Test
    fun `evictLru keeps preserve file even if oldest`() {
        // Given: 三个 100 字节文件，最旧的为待保留文件
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val preserve = createFile(dirA, "preserve.apk", 100, ageMs = 3_000)
        val middle = createFile(dirA, "middle.bin", 100, ageMs = 2_000)
        val newest = createFile(dirA, "newest.bin", 100, ageMs = 1_000)

        // When: 目标 250 字节，preserve 参与统计但不删除
        val freed = manager.evictLru(listOf(dirA), targetBytes = 250L, preserve = preserve)

        // Then: 跳过 preserve，删除次旧的 middle
        assertThat(freed).isEqualTo(100L)
        assertThat(preserve.exists()).isTrue()
        assertThat(middle.exists()).isFalse()
        assertThat(newest.exists()).isTrue()
    }

    // ===== 阈值触发 =====

    @Test
    fun `checkCache returns 0 when under threshold`() {
        // Given: 缓存 100 字节，阈值 200MB
        val manager = createManager()
        createFile(File(tempDir, "cache_a"), "small.bin", 100)

        // When
        val freed = manager.checkCache()

        // Then: 未超阈值，不清理
        assertThat(freed).isEqualTo(0L)
    }

    @Test
    fun `checkCache evicts when over threshold`() {
        // Given: 阈值 1MB（清理目标 0.8MB），缓存 1.2MB
        thresholdMb = 1
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val oldFile = createFile(dirA, "old.bin", 600 * 1024, ageMs = 10_000)
        val newFile = createFile(dirA, "new.bin", 600 * 1024, ageMs = 0)

        // When
        val freed = manager.checkCache()

        // Then: 删除最旧文件后总大小 600KB <= 0.8MB，停止清理
        assertThat(freed).isEqualTo(600L * 1024L)
        assertThat(oldFile.exists()).isFalse()
        assertThat(newFile.exists()).isTrue()
        assertThat(manager.cacheSize()).isAtMost((0.8 * CacheManager.BYTES_PER_MB).toLong())
    }

    @Test
    fun `checkCache returns 0 when threshold unlimited`() {
        // Given: 阈值不限制，缓存超过任何实际大小
        thresholdMb = CacheManager.THRESHOLD_UNLIMITED
        val manager = createManager()
        createFile(File(tempDir, "cache_a"), "big.bin", 10 * 1024 * 1024)

        // When
        val freed = manager.checkCache()

        // Then: 不限制，不清理
        assertThat(freed).isEqualTo(0L)
    }

    @Test
    fun `checkCache returns 0 when auto clean disabled`() {
        // Given: 自动清空关闭，缓存超过阈值
        thresholdMb = 1
        autoCleanEnabled = false
        val manager = createManager()
        createFile(File(tempDir, "cache_a"), "big.bin", 2 * 1024 * 1024)

        // When
        val freed = manager.checkCache()

        // Then: 开关关闭，不清理
        assertThat(freed).isEqualTo(0L)
        assertThat(manager.cacheSize()).isEqualTo(2L * 1024L * 1024L)
    }

    @Test
    fun `checkCache preserves given file`() {
        // Given: 阈值 1MB，缓存 1.2MB，最旧文件为刚下载的 APK
        thresholdMb = 1
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val downloaded = createFile(dirA, "downloaded.apk", 600 * 1024, ageMs = 10_000)
        val newer = createFile(dirA, "newer.bin", 600 * 1024, ageMs = 0)

        // When: preserve 指向刚下载的 APK
        val freed = manager.checkCache(preserve = downloaded)

        // Then: 跳过 preserve 删除 newer，保留文件完整
        assertThat(freed).isEqualTo(600L * 1024L)
        assertThat(downloaded.exists()).isTrue()
        assertThat(newer.exists()).isFalse()
    }

    @Test
    fun `settings change takes effect on next check`() {
        // Given: 阈值 1MB，缓存 600KB
        thresholdMb = 1
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        createFile(dirA, "file.bin", 600 * 1024)

        // When: 首次检查（600KB <= 1MB 不触发），追加文件使缓存增长后（阈值不变）再检查
        val firstFreed = manager.checkCache()
        val older = createFile(dirA, "extra.bin", 600 * 1024, ageMs = 5_000)
        val secondFreed = manager.checkCache()

        // Then: 超过阈值后触发清理，删除较旧的新增文件
        assertThat(firstFreed).isEqualTo(0L)
        assertThat(secondFreed).isEqualTo(600L * 1024L)
        assertThat(older.exists()).isFalse()
        assertThat(manager.cacheSize()).isEqualTo(600L * 1024L)
    }

    // ===== 手动清理 =====

    @Test
    fun `clearAll empties all cache dirs but keeps dirs`() {
        // Given: 两个缓存目录各有文件
        val manager = createManager()
        val dirA = File(tempDir, "cache_a")
        val dirB = File(tempDir, "cache_b")
        createFile(dirA, "a.bin", 100)
        createFile(dirB, "b.bin", 200)

        // When
        manager.clearAll()

        // Then: 文件清空，目录保留
        assertThat(manager.cacheSize()).isEqualTo(0L)
        assertThat(dirA.exists()).isTrue()
        assertThat(dirB.exists()).isTrue()
    }
}
