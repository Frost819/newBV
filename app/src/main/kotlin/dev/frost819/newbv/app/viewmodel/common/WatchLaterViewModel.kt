package dev.frost819.newbv.app.viewmodel.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.frost819.newbv.biliapi.entity.ApiType as BiliApiType
import dev.frost819.newbv.biliapi.repositories.ToViewRepository
import dev.frost819.newbv.data.datastore.ApiType as DataApiType
import dev.frost819.newbv.data.datastore.Prefs
import dev.frost819.newbv.app.util.ToastUtils
import dev.frost819.newbv.core.log.Loggers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WatchLaterEffect {
    data class ShowToast(val message: String) : WatchLaterEffect
}

/**
 * 稍后再看通用 ViewModel。
 *
 * 可在任意页面通过 `hiltViewModel<WatchLaterViewModel>()` 获取，
 * 提供添加/移除稍后再看的能力，无需修改各页面已有的 ViewModel。
 *
 * @param toViewRepository 稍后再看仓库。
 */
@HiltViewModel
class WatchLaterViewModel @Inject constructor(
    private val toViewRepository: ToViewRepository,
) : ViewModel() {

    private val logger = Loggers.get("WatchLaterViewModel")

    private val _effect = MutableSharedFlow<WatchLaterEffect>()
    val effect = _effect.asSharedFlow()

    private fun prefApiType(): BiliApiType = when (Prefs.apiType) {
        DataApiType.Web -> BiliApiType.Web
        DataApiType.App -> BiliApiType.App
    }

    /**
     * 添加视频到稍后再看。
     *
     * @param aid 视频 AV 号。
     * @param bvid 视频 BV 号（可选）。
     */
    fun addToView(aid: Long, bvid: String? = null) {
        viewModelScope.launch {
            runCatching {
                toViewRepository.addToView(
                    aid = aid,
                    bvid = bvid,
                    preferApiType = prefApiType(),
                )
            }.onSuccess {
                _effect.emit(WatchLaterEffect.ShowToast("已添加到稍后再看"))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to add to view" }
                _effect.emit(WatchLaterEffect.ShowToast("添加失败: ${error.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 从稍后再看移除视频。
     *
     * @param aid 视频 AV 号。
     */
    fun delToView(aid: Long) {
        viewModelScope.launch {
            runCatching {
                toViewRepository.delToView(
                    aid = aid,
                    viewed = false,
                    preferApiType = prefApiType(),
                )
            }.onSuccess {
                _effect.emit(WatchLaterEffect.ShowToast("已移除稍后再看"))
            }.onFailure { error ->
                if (error is CancellationException) throw error
                logger.error(error) { "Failed to delete to view" }
                _effect.emit(WatchLaterEffect.ShowToast("移除失败: ${error.message ?: "未知错误"}"))
            }
        }
    }
}

/**
 * 收集 [WatchLaterViewModel] 的 effect 并显示 Toast。
 *
 * 在使用 [WatchLaterViewModel] 的页面中调用此 Composable 即可自动显示 toast。
 */
@Composable
fun CollectWatchLaterEffects(watchLaterViewModel: WatchLaterViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        watchLaterViewModel.effect.collect { effect ->
            when (effect) {
                is WatchLaterEffect.ShowToast -> {
                    ToastUtils.show(context, effect.message)
                }
            }
        }
    }
}
