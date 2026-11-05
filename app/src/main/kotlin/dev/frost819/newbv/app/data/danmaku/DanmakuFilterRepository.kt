package dev.frost819.newbv.app.data.danmaku

import dev.frost819.newbv.biliapi.http.BiliHttpApi
import dev.frost819.newbv.biliapi.repositories.AuthRepository
import dev.frost819.newbv.core.log.Loggers
import dev.frost819.newbv.data.datastore.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账号云端弹幕屏蔽规则（关键词 / 正则 / 用户）的应用级缓存。
 *
 * 规则与账号绑定，App 启动时在后台预热（见 `BVApplication`），播放器/直播只读取缓存，
 * 不再各自发起请求，也不阻塞首段弹幕加载。
 *
 * 生命周期：Hilt `@Singleton`，随进程存活；账号切换/登出由 [onAccountChanged] 失效。
 *
 * 线程：`ensureLoaded` 在 IO/Default 线程写，[shouldDrop] / [shouldDropLive] 分别被
 * 点播过滤（Default）与直播回调（Main）读取，快照不可变且 `@Volatile`。
 */
@Singleton
class DanmakuFilterRepository
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        private val logger = Loggers.get("DanmakuFilterRepository")

        /**
         * 规则拉取使用的协程作用域。
         *
         * 默认随进程存活（IO）；单元测试可替换为测试调度器以配合 `advanceUntilIdle`。
         */
        internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** 当前规则快照；[DanmakuFilterRules.EMPTY] 表示无规则或尚未加载。 */
        @Volatile private var rules: DanmakuFilterRules = DanmakuFilterRules.EMPTY

        /** [rules] 对应的账号 mid；null 表示尚未成功加载（或已登出）。 */
        @Volatile private var loadedMid: Long? = null

        /** 串行化加载：并发 [ensureLoaded] 只发一次请求，等锁方复用结果。 */
        private val loadMutex = Mutex()

        private val _ruleCount = MutableStateFlow(0)

        /** 已加载的规则条数（仅启用的规则），用于菜单展示。 */
        val ruleCount: StateFlow<Int> = _ruleCount.asStateFlow()

        private val _version = MutableStateFlow(0)

        /**
         * 规则版本：每次成功加载或账号失效时自增。
         *
         * 播放器据此判断「规则是否在分段加载之后才就绪」，必要时重载当前分段。
         */
        val version: StateFlow<Int> = _version.asStateFlow()

        /** 当前是否已有可用规则（用于避免空规则时的无意义重载）。 */
        val hasRules: Boolean
            get() = !rules.isEmpty

        /**
         * 后台预热：异步拉取当前账号规则，失败静默降级。
         *
         * 供 App 启动、进入播放器/直播时调用，不阻塞调用方。
         */
        fun prefetch() {
            scope.launch { ensureLoaded() }
        }

        /**
         * 确保当前账号规则已就绪。
         *
         * 已加载（或已确认无规则）的同一账号立即返回；否则拉取一次，超时/失败返回 false。
         * 未登录时不发请求。
         *
         * @return true 表示当前账号规则已就绪（可能为空规则集）
         */
        suspend fun ensureLoaded(): Boolean {
            val mid = authRepository.mid
            if (mid == null) {
                clearRules()
                return false
            }
            // 快路径：正常播放中每次切集都会调用，避免进入锁
            if (loadedMid == mid) return true
            return loadMutex.withLock {
                // 等锁期间可能已被其他调用加载，或账号已切换/登出
                val currentMid = authRepository.mid
                if (currentMid == null) {
                    clearRules()
                    return@withLock false
                }
                if (loadedMid == currentMid) return@withLock true
                load(currentMid)
            }
        }

        private suspend fun load(mid: Long): Boolean =
            try {
                val fetched =
                    withTimeout(USER_FILTER_TIMEOUT_MS) {
                        BiliHttpApi.getDanmakuFilterRules()
                    }
                // 请求期间可能切换账号/登出：过期结果不能写入
                if (authRepository.mid != mid) {
                    logger.info { "Discard danmaku filter rules: account changed during load" }
                    return false
                }
                val enabledRules = fetched.filter { it.opened }
                val compiled = compileDanmakuFilterRules(enabledRules)
                rules = compiled
                loadedMid = mid
                _ruleCount.value = enabledRules.size
                _version.update { it + 1 }
                logger.info {
                    "Loaded danmaku filter rules: total=${fetched.size}, " +
                        "enabled=${enabledRules.size}, keyword=${compiled.keywords.size}, " +
                        "regex=${compiled.regexes.size}, user=${compiled.blockedUserHashes.size}, " +
                        "invalidRegex=${compiled.invalidRegexCount}"
                }
                true
            } catch (e: TimeoutCancellationException) {
                logger.warn { "Load danmaku filter rules timeout after ${USER_FILTER_TIMEOUT_MS}ms" }
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn { "Load danmaku filter rules failed: $e" }
                false
            }

        /**
         * 点播弹幕是否应丢弃（命中账号屏蔽规则）。
         *
         * 本地「账号屏蔽规则」开关关闭时恒返回 false；点播与直播共用同一个判断入口。
         *
         * @param midHash 弹幕发送者 midHash（接口下发）
         * @param content 弹幕文本
         */
        fun shouldDrop(
            midHash: String,
            content: String,
        ): Boolean =
            Prefs.defaultDanmakuBlockByUserFilterEnabled &&
                rules.matches(midHash = midHash, content = content)

        /**
         * 直播弹幕是否应丢弃（WS 只提供原始 mid）。
         *
         * 本地「账号屏蔽规则」开关关闭时恒返回 false。
         *
         * @param content 弹幕文本
         * @param mid 发送者 UID
         */
        fun shouldDropLive(
            content: String,
            mid: Long,
        ): Boolean =
            Prefs.defaultDanmakuBlockByUserFilterEnabled &&
                rules.matchesLive(content = content, mid = mid)

        /**
         * 账号变更通知：换号/登录时异步重新预热，登出时清空缓存。
         *
         * 即使调用方遗漏本方法，[ensureLoaded] 也会在下次调用时比对 mid 自行刷新。
         *
         * @param mid 新账号 mid；null 表示已登出
         */
        fun onAccountChanged(mid: Long?) {
            if (mid != null && mid == loadedMid) return
            clearRules()
            if (mid != null) prefetch()
        }

        private fun clearRules() {
            if (loadedMid == null && rules.isEmpty) return
            rules = DanmakuFilterRules.EMPTY
            loadedMid = null
            _ruleCount.value = 0
            _version.update { it + 1 }
        }

        private companion object {
            /** 规则拉取超时（毫秒）；超时降级为「不过滤」，等待下次进入播放器/直播重试。 */
            const val USER_FILTER_TIMEOUT_MS = 3_000L
        }
    }
