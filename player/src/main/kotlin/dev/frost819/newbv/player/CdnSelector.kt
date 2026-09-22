package dev.frost819.newbv.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * CDN 地址选择器。
 *
 * 负责对候选播放地址进行测速并给出推荐顺序，供播放器起播前选择最优节点。
 */
interface CdnSelector {
    /**
     * 按实测吞吐对候选地址从高到低排序。
     *
     * 已缓存且未过期的 host 直接使用缓存分数，其余 host 并发做小流量测速；
     * 测速失败的地址分数记为 0，保持其在入参中的相对顺序排在末尾。
     *
     * @param urls 候选播放地址（同一视频/音频轨道的 base + backup 列表）。
     * @return 排序后的地址列表；入参为空时返回空列表。
     */
    suspend fun rank(urls: List<String>): List<String>
}

/**
 * 基于 OkHttp Range 请求的 CDN 测速选择器。
 *
 * 对每个候选地址发起 `Range: bytes=0-<sampleBytes-1>` 请求，测量读取样本所需的
 * 时间并换算为吞吐（字节/秒）。结果按 host 缓存 [cacheTtlMs]，避免同一 host 反复测速。
 *
 * @param client 复用的 OkHttpClient，内部会派生一个短超时的测速专用 client。
 * @param cacheTtlMs 测速结果缓存有效期。
 * @param sampleBytes 每次测速读取的样本字节数。
 * @param measureTimeoutMs 单个地址的测速超时。
 */
class OkHttpCdnSelector(
    client: OkHttpClient,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val sampleBytes: Long = DEFAULT_SAMPLE_BYTES,
    measureTimeoutMs: Long = DEFAULT_MEASURE_TIMEOUT_MS,
) : CdnSelector {
    private data class HostScore(
        val bytesPerSecond: Long,
        val measuredAtMs: Long,
    )

    private val hostScores = ConcurrentHashMap<String, HostScore>()

    private val measureClient =
        client
            .newBuilder()
            .connectTimeout(measureTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(measureTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(measureTimeoutMs, TimeUnit.MILLISECONDS)
            .build()

    override suspend fun rank(urls: List<String>): List<String> {
        val candidates = urls.filter { it.isNotBlank() }.distinct()
        if (candidates.size <= 1) return candidates

        val now = System.currentTimeMillis()
        val stale =
            candidates.filter { url ->
                val score = hostScores[hostOf(url)]
                score == null || now - score.measuredAtMs > cacheTtlMs
            }
        if (stale.isNotEmpty()) {
            coroutineScope {
                stale
                    .map { url -> async(Dispatchers.IO) { url to measure(url) } }
                    .awaitAll()
                    .forEach { (url, score) ->
                        if (score > 0) {
                            hostScores[hostOf(url)] = HostScore(score, System.currentTimeMillis())
                        }
                    }
            }
        }

        // sortedByDescending 使用稳定排序，未测得（分数 0）的地址保持入参相对顺序
        return candidates.sortedByDescending { hostScores[hostOf(it)]?.bytesPerSecond ?: 0L }
    }

    /** 对单个地址做 Range 测速，返回吞吐（字节/秒）；失败返回 0。 */
    private fun measure(url: String): Long {
        val startedAtNs = System.nanoTime()
        return try {
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("Range", "bytes=0-${sampleBytes - 1}")
                    .header("User-Agent", MEASURE_USER_AGENT)
                    .header("Referer", "https://www.bilibili.com")
                    .build()
            measureClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0L
                val body = response.body ?: return 0L
                val source = body.source()
                val buffer = Buffer()
                var total = 0L
                while (total < sampleBytes) {
                    val read = source.read(buffer, sampleBytes - total)
                    if (read == -1L) break
                    total += read
                    buffer.clear()
                }
                val elapsedNs = System.nanoTime() - startedAtNs
                if (total <= 0 || elapsedNs <= 0) 0L else total * 1_000_000_000L / elapsedNs
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun hostOf(url: String): String = runCatching { URI(url).host ?: url }.getOrDefault(url)

    companion object {
        /** 测速结果缓存有效期：10 分钟。 */
        const val DEFAULT_CACHE_TTL_MS = 10 * 60 * 1000L

        /** 默认样本字节数：128 KB。 */
        const val DEFAULT_SAMPLE_BYTES = 128L * 1024

        /** 默认单地址测速超时：2.5 秒。 */
        const val DEFAULT_MEASURE_TIMEOUT_MS = 2_500L

        private const val MEASURE_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
